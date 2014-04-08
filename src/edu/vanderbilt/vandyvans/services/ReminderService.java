package edu.vanderbilt.vandyvans.services;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import edu.vanderbilt.vandyvans.R;
import edu.vanderbilt.vandyvans.models.ArrivalTime;
import edu.vanderbilt.vandyvans.models.Stops;

import static edu.vanderbilt.vandyvans.services.Global.APP_LOG_ID;

/**
 * The Service that queries the Syncromatics server for arrival times.
 *
 * When a van arrives, publish the result and create a Notification.
 *
 * Created by athran on 4/6/14.
 */
public class ReminderService extends Service implements Handler.Callback {

    public static final int SUBSCRIBE_TO_STOP   = 42;
    public static final int UNSUBSCRIBE_TO_STOP = 43;
    public static final int VAN_IS_ARRIVEN      = 44;
    public static final int ADD_CLIENT          = 45;
    public static final int REMOVE_CLIENT       = 46;

    private static final int STOP_TRACKING = 47;
    private static final int INIT          = 48;

    private HandlerThread workerThread;
    private Messenger     clientHandler;
    private Handler       trackerHandler;
    private Handler       syncroHandler;

    private WeakReference<Messenger> mSingleClient = null;
    private Set<StopTracker>         mTrackers     = new HashSet<StopTracker>();
    private int                      mReminderId   = 1;

    @Override
    public void onCreate() {
        logMessage("Service is created");
        workerThread = new HandlerThread("reminderbackgroundthread");
        workerThread.start();

        syncroHandler  = new Handler(workerThread.getLooper(), new SyncromaticsClient());
        clientHandler  = new Messenger(new Handler(this));
        trackerHandler = new Handler(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        logMessage("Service is bound");
        return clientHandler.getBinder();
    }

    @Override
    public void onDestroy() {
        logMessage("Service is destroyed");
        workerThread.quit();
    }

    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case SUBSCRIBE_TO_STOP:
                logMessage("Received subscription request: " +  message.arg1);
                StopTracker tracker = new StopTracker(message.arg1,
                                                      trackerHandler,
                                                      syncroHandler);
                Message.obtain(tracker, INIT)
                        .sendToTarget();
                mTrackers.add(tracker);
                break;

            case UNSUBSCRIBE_TO_STOP:
                logMessage("Received unsubscription request: " +  message.arg1);
                StopTracker toBeStopped = null;
                for (StopTracker trkr : mTrackers) {
                    if (trkr.stopId == message.arg1) {
                        toBeStopped = trkr;
                    }
                }

                if (toBeStopped != null) {
                    mTrackers.remove(toBeStopped);
                    Message.obtain(toBeStopped, STOP_TRACKING)
                            .sendToTarget();
                }
                break;

            case ADD_CLIENT:
                // keep just a single client for now
                mSingleClient = new WeakReference<Messenger>(message.replyTo);
                break;

            case REMOVE_CLIENT:
                mSingleClient = null;
                break;

            case VAN_IS_ARRIVEN:
                StopTracker reportingTracker = (StopTracker)message.obj;
                mTrackers.remove(reportingTracker);
                doBroadcastVanArrival(reportingTracker.latestArrivalTime);

                if (mTrackers.isEmpty()) { stopSelf(); }
                break;
        }
        return true;
    }

    private void doBroadcastVanArrival(ArrivalTime arrivalTime) {
        Messenger client = mSingleClient.get();
        if (client != null) {
            try {
                client.send(Message.obtain(null,
                                           VAN_IS_ARRIVEN,
                                           arrivalTime.stop.id,
                                           0));
                logMessage("notified client");
            } catch (RemoteException e) {
                logMessage("Failed to communicate with the client");
            }
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        logMessage("About to broadcast for: " + arrivalTime.toString());
        mReminderId++;
        notificationManager.notify(
                mReminderId,
                new Notification.Builder(this)
                        .setContentTitle("Van Arriving")
                        .setContentText(new StringBuilder("The ")
                                                .append(arrivalTime.route.name)
                                                .append(" Route will be arriving at ")
                                                .append(arrivalTime.stop.name)
                                                .append(" in 5 minutes")
                                                .toString())
                        .setSmallIcon(R.drawable.van_icon)
                        .getNotification());
    }

    private static void logMessage(String msg) {
        Log.i(APP_LOG_ID, "ReminderService | " + msg);
    }

    private static final class StopTracker extends Handler {
        final int     stopId;
        final Handler syncroClient;
        final Handler parent;

        boolean     isTracking;
        ArrivalTime latestArrivalTime;

        private static final int NOTIFY_PARENT = 55;

        StopTracker(int     _id,
                    Handler _parent,
                    Handler _syncroClient) {
            super();
            stopId       = _id;
            parent       = _parent;
            syncroClient = _syncroClient;

            isTracking = true;
        }

        @Override public void handleMessage(Message msg) {
            if (msg.what == INIT) {
                logMessage("Initing for stopd: " + stopId);
                isTracking = true;
                Message.obtain(syncroClient, 0,
                               new Global.FetchArrivalTimes(this,
                                                            Stops.getForId(stopId)))
                        .sendToTarget();
            }
            if (msg.what == STOP_TRACKING) {
                isTracking = false;
            }
            if (msg.what == NOTIFY_PARENT) {
                if (isTracking) {
                    logMessage("Notifying parent");
                    Message.obtain(parent,
                                   VAN_IS_ARRIVEN,
                                   this).sendToTarget();
                }
            }
            if (msg.obj instanceof Global.ArrivalTimeResults) {
                handleArrivalTimes(((Global.ArrivalTimeResults) msg.obj).times);
            }
        }

        private void handleArrivalTimes(List<ArrivalTime> times) {
            logMessage("Received this many ArrivalTimes: " + times.size());
            for (ArrivalTime time : times) {
                logMessage(time.toString());
            }

            if (times.isEmpty()) { return; }

            latestArrivalTime = Collections.min(times, new Comparator<ArrivalTime>() {
                @Override
                public int compare(ArrivalTime lhs, ArrivalTime rhs) {
                    return lhs.minutes - rhs.minutes;
                }
            });

            if (latestArrivalTime.minutes > 5) {
                logMessage("Scheduling delayed notification");
                logMessage(latestArrivalTime.toString());
                sendMessageDelayed(Message.obtain(this, NOTIFY_PARENT),
                                   (latestArrivalTime.minutes - 5) * 60000);
            } else {
                logMessage("Scheduling immediate notification");
                logMessage(latestArrivalTime.toString());
                Message.obtain(this, NOTIFY_PARENT).sendToTarget();
            }
        }

    }

}
