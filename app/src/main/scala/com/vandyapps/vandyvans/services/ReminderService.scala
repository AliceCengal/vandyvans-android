package com.vandyapps.vandyvans.services

import scala.ref.WeakReference
import android.app.{Notification, NotificationManager, Service}
import android.content.{Context, Intent}
import android.os._
import android.util.Log
import com.vandyapps.vandyvans.R
import com.vandyapps.vandyvans.models.{Stop, ArrivalTime}
import com.cengallut.handlerextension.HandlerExt
import com.vandyapps.vandyvans.client.VansServerCalls

private[services] class ReminderService extends Service with Handler.Callback {
  import ReminderService._

  lazy val workerThread = new HandlerThread("reminderThread")
  lazy val syncroHandler = new Handler(workerThread.getLooper) //, new SyncromaticsClient)
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
        logMessage(s"Received subscription request: ${msg.arg1}")
        val tracker = new StopTracker(msg.arg1, trackerHandler, syncroHandler)
        Message.obtain(tracker, INIT).sendToTarget()
        trackers += tracker

      case UNSUBSCRIBE_TO_STOP =>
        logMessage(s"Received unsubscription request: ${msg.arg1}")
        trackers.find(_.id == msg.arg1).foreach { tracker =>
          Message.obtain(tracker, STOP_TRACKING).sendToTarget()
          trackers -= tracker
        }

      case ADD_CLIENT =>
        singleClient = Some(WeakReference(msg.replyTo))

      case REMOVE_CLIENT =>
        singleClient = None

      case VAN_IS_ARRIVEN =>
        val reportingTracker = msg.obj.asInstanceOf[StopTracker]
        trackers -= reportingTracker
        reportingTracker.latestArrivalTime.foreach { doBroadcastVanArrival }
        if (trackers.isEmpty) { stopSelf() }
    }
    true
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
    reminderId += 1
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

object ReminderService {
  val SUBSCRIBE_TO_STOP   = 42
  val UNSUBSCRIBE_TO_STOP = 43
  val VAN_IS_ARRIVEN      = 44
  val ADD_CLIENT          = 45
  val REMOVE_CLIENT       = 46
  val NOTIFY_PARENT       = 55

  private val STOP_TRACKING = 47
  private val INIT          = 48

  def logMessage(msg: String) {
    Log.i(Global.APP_LOG_ID, "ReminderService | " + msg)
  }

  def stopTracker(id: Int,
                  parent: HandlerExt,
                  services: VansServerCalls): HandlerExt = {
    handler() {
      case _ =>
    }
  }

  class StopTracker(val id: Int,
                    val parent: Handler,
                    val syncro: Handler)
    extends Handler
  {
    var isTracking = true
    var latestArrivalTime = Option.empty[ArrivalTime]
    implicit val handler: Handler = this

    override def handleMessage(msg: Message) {
      if (msg.what == INIT) {
        logMessage(s"Initing for stopId: $id")
        isTracking = true
        //Stop.forId(id).foreach { stop => syncro ? FetchArrivalTimes(stop) }


      } else if (msg.what == NOTIFY_PARENT) {
        if (isTracking) {
          logMessage("Notifying parent")
          Message.obtain(parent, VAN_IS_ARRIVEN, this).sendToTarget()
        }

      } else {
        //msg.obj match { case o: ArrivalTimeResults =>
        //  handleArrivalTimes(o.times)
        //}

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
            logMessage(latest.toString)
            sendMessageDelayed(Message.obtain(this, NOTIFY_PARENT), (latest.minutes - 5) * 60000)
          } else {
            logMessage("Scheduling immediate notification")
            logMessage(latest.toString)
            Message.obtain(this, NOTIFY_PARENT).sendToTarget()
          }
        }
      }
    }
  }
}