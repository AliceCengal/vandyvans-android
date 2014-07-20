package edu.vanderbilt.vandyvans.services

import scala.util.Try
import android.content.{ComponentName, Intent, Context, ServiceConnection}
import android.os._
import android.util.Log

class SimpleReminderController(ctx: Context) extends ReminderController
                                       with ServiceConnection
                                       with Handler.Callback
{
  import SimpleReminderController._

  private val prefs = ctx.getSharedPreferences(Global.APP_PREFERENCES, Context.MODE_PRIVATE)

  private var subscribedStops = Set.empty[Int]
  private var isServiceRunning = false
  private var serviceHandler = Option.empty[Messenger]
  private lazy val receiver = new Messenger(new Handler(this))

  def start() {
    subscribedStops ++ str2Nums(prefs.getString(REMINDER_SUBSCRIPTION, ""))

    if (subscribedStops.nonEmpty) doBindService()

  }

  override def subscribeReminderForStop(stopId: Int) {
    subscribedStops = subscribedStops + stopId
    recordSubscription()
    if (!isServiceRunning) {
      doBindService()
    } else {
      doSubscribe(stopId)
    }
  }

  override def unsubscribeReminderForStop(stopId: Int) {
    subscribedStops = subscribedStops - stopId
    recordSubscription()
    if (isServiceRunning) doUnsubscribe(stopId)
  }

  override def isSubscribedToStop(stopId: Int) = subscribedStops.contains(stopId)

  override def onServiceConnected(componentName: ComponentName, service: IBinder) {
    logMessage("Service is connected")
    isServiceRunning = true
    serviceHandler = Some(new Messenger(service))

    try {
      val msg = Message.obtain(null, ReminderService.ADD_CLIENT, 0, 0)
      msg.replyTo = receiver
      serviceHandler.foreach(_.send(msg))
    } catch {
      case _: RemoteException => logMessage("Failed to communicate to ReminderService")
    }

    subscribedStops.foreach { stop =>
      doSubscribe(stop)
    }
  }

  override def onServiceDisconnected(componentName: ComponentName) {
    logMessage("Service is disconnected")
    isServiceRunning = false
    serviceHandler = None
  }

  override def handleMessage(msg: Message) = {
    if (msg.what == ReminderService.VAN_IS_ARRIVEN) {
      subscribedStops = subscribedStops - msg.arg1
      recordSubscription()
      logMessage(s"Received van arrival message for stop: ${msg.arg1}")
    }
    true
  }

  private def recordSubscription() {
    prefs.edit().putString(REMINDER_SUBSCRIPTION, nums2Str(subscribedStops)).apply()
  }

  private def doBindService() {
    logMessage("Binding to ReminderService")
    ctx.bindService(new Intent(ctx, classOf[ReminderService]), this, Context.BIND_AUTO_CREATE)
  }

  private def doSubscribe(stop: Int) {
    serviceHandler.foreach { messenger =>
      try {
        messenger.send(Message.obtain(null, ReminderService.SUBSCRIBE_TO_STOP, stop, 0))
      } catch {
        case _: RemoteException => logMessage("Failed to communicate with ReminderService")
      }
    }
  }

  private def doUnsubscribe(stop: Int) {
    serviceHandler.foreach { messenger =>
      try {
        messenger.send(Message.obtain(null, ReminderService.UNSUBSCRIBE_TO_STOP, stop, 0))
      } catch {
        case _: RemoteException => logMessage("Failed to communicate with ReminderService")
      }
    }
  }

}

object SimpleReminderController {

  val REMINDER_SUBSCRIPTION = "reminderSubsctiontion"

  def str2Nums(str: String) =
    str.split(",").flatMap(s => Try(s.toInt).toOption).toSet

  def nums2Str(nums: Set[Int]) = nums.mkString(",")

  def logMessage(msg: String) { Log.d(Global.APP_LOG_ID, s"ReminderController | $msg") }

}