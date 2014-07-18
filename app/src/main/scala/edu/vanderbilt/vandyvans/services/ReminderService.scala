package edu.vanderbilt.vandyvans.services

import android.app.{Notification, NotificationManager, Service}
import android.content.{Context, Intent}
import android.os._
import android.util.Log
import com.marsupial.eventhub.ActorConversion
import edu.vanderbilt.vandyvans.models.{Stops, ArrivalTime}

import scala.ref.WeakReference

class ReminderServices extends Service with Handler.Callback {
  import ReminderServices._

  lazy val workerThread = new HandlerThread("reminderThread")
  lazy val syncroHandler = new Handler(workerThread.getLooper, new SyncromaticsClient)
  lazy val clientHandler = new Messenger(new Handler(this))
  lazy val trackerHandler = new Handler(this)

  var singleClient = Option.empty[WeakReference[Messenger]]
  var trackers = Set.empty[StopTracker]
  var reminderId = 1

  override def onCreate() {
    logMessage("Service is created")
    workerThread.start()
  }

  override def onBind(intent: Intent) = {
    logMessage("Service is bound")
    clientHandler.getBinder
  }

  override def onDestroy() {
    logMessage("Service is destroyed")
    workerThread.quit()
  }

  override def handleMessage(msg: Message) = {
    msg.what match {
      case SUBSCRIBE_TO_STOP =>
        logMessage(s"Received subscription request: ${message.arg1}")
        val tracker = new StopTracker(message.arg1, trackerHandler, syncroHandler)
        Message.obtain(tracker, INIT).sendToTarget()
        trackers = trackers + tracker

      case UNSUBSCRIBE_TO_STOP =>
        logMessage("Received unsubscription request: " + message.arg1)
        trackers.find(_.id == message.arg1).foreach { tracker =>
          Message.obtain(tracker, STOP_TRACKING).sendToTarget()
          trackers = trackers - tracker
        }

      case ADD_CLIENT =>
        singleClient = Some(WeakReference(msg.replyTo))

      case REMOVE_CLIENT =>
        singleClient = None

      case VAN_IS_ARRIVEN =>
        val reportingTracker = msg.obj.asInstanceOf[StopTracker]
        trackers = tracker - reportingTracker
        doBroadcastVanArrival(reportingTracker.latestArrivalTime)
        if (trackers.isEmpty) { stopSelf() }
    }
  }

  private def doBroadcastVanArrival(arrivalTime: ArrivalTime) {
    for (clientRef <- singleClient;
         client <- clientRef.get) {
      try {
        client.send(Message.obtain(null, VAN_IS_ARRIVEN, arrivalTime.stop.id, 0))
        logMessage("Notified client")
      } catch {
        case _: RemoteException => logMessage("Failed to communicate with the client")
      }
    }

    val notificationMan = getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]
    logMessage(s"About to broadcast for: ${arrivalTime.toString}")
    reminderId = reminderId + 1
    notificationMan.notify(
      reminderId,
      new Notification.Builder(this)
        .setContentTitle("Van Arriving")
        .setContentText(
          s"The ${arrivalTime.route.name} Route will be arriving at " +
          s"${arrivalTime.stop.name} in 5 minutes")
        .setSmallIcon(R.drawable.van_icon)
        .build())
  }

}

object ReminderServices {
  val SUBSCRIBE_TO_STOP: Int = 42
  val UNSUBSCRIBE_TO_STOP: Int = 43
  val VAN_IS_ARRIVEN: Int = 44
  val ADD_CLIENT: Int = 45
  val REMOVE_CLIENT: Int = 46
  private val STOP_TRACKING: Int = 47
  private val INIT: Int = 48
  val NOTIFY_PARENT = 55

  def logMessage(msg: String) {
    Log.i(Global.APP_LOG_ID, "ReminderService | " + msg)
  }

  class StopTracker(val id: Int,
                    val parent: Handler,
                    val syncro: Handler)
    extends Handler with ActorConversion
  {
    var isTracking = true
    var latestArrivalTime = Option.empty[ArrivalTime]

    override def handleMessage(msg: Message) {
      if (msg.what == INIT) {
        logMessage("Initing for stopId: " + stopId)
        isTracking = true
        syncro ! new SyncromaticsClient.FetchArrivalTimes(this, Stops.getForId(id))

      } else if (msg.what == NOTIFY_PARENT) {
        if (isTracking) {
          logMessage("Notifying parent")
          Message.obtain(parent, VAN_IS_ARRIVEN, this).sendToTarget()
        }

      } else if (msg.obj.isInstanceOf[SyncromaticsClient.ArrivalTimeResults]) {
        handleArrivalTimes(msg.obj.times)
      }
    }

    private def handleArrivalTimes(times: Iterable[ArrivalTime]) {
      logMessage("Received this many ArrivalTimes: " + times.size)
      times.foreach { t => logMessage(t.toString) }

      if (times.nonEmpty) {
        latestArrivalTime = Some(times.toSeq.sortBy(_.minutes).head)
        for (latest <- latestArrivalTime) {
          if (latest.minutes > 5) {
            logMessage("Scheduling delayed notification")
            logMessage(latestArrivalTime.toString)
            sendMessageDelayed(Message.obtain(this, NOTIFY_PARENT), (latestArrivalTime.minutes - 5) * 60000)
          } else {
            logMessage("Scheduling immediate notification")
            logMessage(latestArrivalTime.toString)
            Message.obtain(this, NOTIFY_PARENT).sendToTarget()
          }
        }
      }
    }
  }
}