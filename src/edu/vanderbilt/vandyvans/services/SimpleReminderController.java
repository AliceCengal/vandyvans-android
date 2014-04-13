package edu.vanderbilt.vandyvans.services;

import java.util.HashSet;
import java.util.Set;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import edu.vanderbilt.vandyvans.ReminderController;

import static edu.vanderbilt.vandyvans.services.Global.APP_LOG_ID;

/**
 * Implementation of the ReminderController interface.
 *
 * Created by athran on 4/6/14.
 */
class SimpleReminderController implements ReminderController,
                                          ServiceConnection,
                                          Handler.Callback {

    private static final String REMINDER_SUBSCRIPTION = "remindersubscription";

    private final Context           mCtx;
    private final SharedPreferences mPrefs;

    private Set<Integer> subscribedStops  = new HashSet<Integer>();
    private boolean      isServiceRunning = false;
    private Messenger    serviceHandler   = null;
    private Messenger    receiver         = null;

    SimpleReminderController(Context ctx) {
        mCtx = ctx;
        mPrefs = mCtx.getSharedPreferences(Global.APP_PREFERENCES,
                                           Context.MODE_PRIVATE);
    }

    void start() {
        subscribedStops.addAll(
                decodeIntegerSetFromString(
                        mPrefs.getString(
                                REMINDER_SUBSCRIPTION,
                                "")));

        if (!subscribedStops.isEmpty()) {
            doBindService();
        }

        receiver = new Messenger(new Handler(this));
    }

    /**
     * implementation from ReminderController
     */
    public void subscribeReminderForStop(int stopId) {
        subscribedStops.add(stopId);
        recordSubscription();
        if (!isServiceRunning) {
            doBindService();
        } else {
            doSubscribeForStop(stopId);
        }
    }

    /**
     * implementation from ReminderController
     */
    public void unsubscribeReminderForStop(int stopId) {
        subscribedStops.remove(stopId);
        recordSubscription();
        if (isServiceRunning) {
            doUnsubscribeForStop(stopId);
        }
    }

    /**
     * implementation from ReminderController
     */
    public boolean isSubscribedToStop(int stopId) {
        return subscribedStops.contains(Integer.valueOf(stopId));
    }

    /**
     * Implementation from ServiceConnection
     */
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        logMessage("Service is connected");
        isServiceRunning = true;
        serviceHandler   = new Messenger(service);

        try {
            Message msg = Message.obtain(null, ReminderService.ADD_CLIENT, 0, 0);
            msg.replyTo = receiver;
            serviceHandler.send(msg);

        } catch (RemoteException e) {
            logMessage("Failed to communicate to ReminderService");
        }

        for (Integer stopId : subscribedStops) {
            doSubscribeForStop(stopId);
        }

    }

    /**
     * Implementation from ServiceConnection
     */
    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        logMessage("Service is disconnected");
        isServiceRunning = false;
        serviceHandler   = null;
    }

    /**
     * Receive messages from the ReminderService. See `onServiceConnected`
     *
     * Implementation from Handler.Callback
     */
    @Override
    public boolean handleMessage(Message message) {
        if (message.what == ReminderService.VAN_IS_ARRIVEN) {
            subscribedStops.remove(message.arg1);
            recordSubscription();
            logMessage("Received van arrival message for stop: " + message.arg1);
        }
        return true;
    }

    private void recordSubscription() {
        mPrefs.edit()
                .putString(REMINDER_SUBSCRIPTION,
                           encodeIntegerSetToString(subscribedStops))
                .apply();
    }

    private void doBindService() {
        logMessage("Binding to ReminderService");
        mCtx.bindService(new Intent(mCtx,
                                    ReminderService.class),
                         this,
                         Context.BIND_AUTO_CREATE);
    }

    /**
     * Send message to the ReminderService's handler. Make sure to check
     * that the service is running.
     */
    private void doSubscribeForStop(int stopId) {
        if (serviceHandler == null) {
            logMessage("serviceHandler is null");
            return;
        }

        try {
            serviceHandler.send(
                    Message.obtain(
                            null,
                            ReminderService.SUBSCRIBE_TO_STOP,
                            stopId,
                            0)
            );

        } catch (RemoteException e) {
            logMessage("Failed to communicate with ReminderService");
        }
    }

    private void doUnsubscribeForStop(int stopId) {
        if (serviceHandler == null) {
            logMessage("serviceHandler is null");
            return;
        }

        try {
            serviceHandler.send(
                    Message.obtain(
                            null,
                            ReminderService.UNSUBSCRIBE_TO_STOP,
                            stopId,
                            0)
            );
        } catch (RemoteException e) {
            logMessage("Failed to communicate with ReminderService");
        }
    }

    private static Set<Integer> decodeIntegerSetFromString(String encoded) {
        logMessage("decoding: " + encoded);
        Set<Integer> decodedSet = new HashSet<Integer>();

        if (!encoded.equals("")) {
            for (String elem : encoded.split(",")) {
                try {
                    int decoded = Integer.parseInt(elem.trim());
                    decodedSet.add(decoded);
                } catch (NumberFormatException e) {
                    // don't add anything
                }
            }
        }

        logMessage("decoded this many integers: " + Integer.toString(decodedSet.size()));
        return decodedSet;
    }

    private static String encodeIntegerSetToString(Set<Integer> integerSet) {
        logMessage("encoding this many integers: " + Integer.toString(integerSet.size()));
        StringBuilder encodedString = new StringBuilder();

        for (Integer num : integerSet) {
            encodedString
                    .append(Integer.toString(num))
                    .append(",");
        }
        if (!integerSet.isEmpty()) { encodedString.deleteCharAt(encodedString.length()-1); }

        logMessage("encoded: " + encodedString.toString());
        return encodedString.toString();
    }

    private static void logMessage(String msg) {
        Log.d(APP_LOG_ID, "ReminderController | " + msg);
    }

}
