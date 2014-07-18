package edu.vanderbilt.vandyvans

import android.app.Activity
import android.content.{Intent, Context}
import android.os.{Message, Bundle, Handler}
import android.view.View
import android.widget._

import com.marsupial.eventhub.Helpers.EasyActivity
import edu.vanderbilt.vandyvans.models.{Routes, ArrivalTime, Stops, Stop}
import edu.vanderbilt.vandyvans.services.{Clients, SyncromaticsClient}

class DetailActivity extends Activity with EasyActivity with Handler.Callback {
  import DetailActivity._

  def blueRL    = component[RelativeLayout](R.id.rl1)
  def blueDisp  = component[TextView](R.id.tv1)
  def redRL     = component[RelativeLayout](R.id.rl2)
  def redDisp   = component[TextView](R.id.tv2)
  def greenRL   = component[RelativeLayout](R.id.rl2)
  def greenDisp = component[TextView](R.id.tv2)

  lazy val blueGroup  = new ArrivalTimeViewHolder(blueRL, blueDisp)
  lazy val redGroup   = new ArrivalTimeViewHolder(redRL, redDisp)
  lazy val greenGroup = new ArrivalTimeViewHolder(greenRL, greenDisp)

  def arrivalLoading = component[ProgressBar](R.id.progress1)
  def failureText    = component[TextView](R.id.tv4)
  def reminderText   = component[TextView](R.id.tv5)
  def reminderSwitch = component[Switch](R.id.cb1)

  lazy val reminderViewController =
    new ReminderViewController(reminderSwitch, reminderText, this)
  lazy val controller = new Handler(this)
  var stop: Stop = null
  val apiClient: Clients = null
  val reminderController: ReminderController = null

  override def onCreate(saved: Bundle) {
    super.onCreate(saved)
    setContentView(R.layout.activity_stopdetail)

    blueGroup.hide()
    redGroup.hide()
    greenGroup.hide()
    failureText.setVisibility(View.GONE)

    val stopId =
      getIntent.getIntExtra(TAG_ID, 0) match {
        case 0 => throw new IllegalStateException(
          "No Stop to be detailed. Why do you even call me?")
        case n => n
      }

    stop = Stops.getForId(stopId)
    Option(getActionBar).foreach { _.setTitle(stop.name) }

    Message.obtain(apiClient.syncromatics(), 0,
                   new SyncromaticsClient.FetchArrivalTimes(controller, stop))
      .sendToTarget()
  }

  override def handleMessage(msg: Message) = {
    msg.obj match {
      case x: SyncromaticsClient.ArrivalTimeResults =>
        displayArrivalTimes(x.times)
    }
    false
  }

  def displayArrivalTimes(times: Seq[ArrivalTime]) {
    arrivalLoading.setVisibility(View.GONE)
    if (times.isEmpty) {
      failureText.setVisibility(View.VISIBLE)
    } else {
      times.foreach { time =>
        time.route match {
          case Routes.BLUE =>
            blueGroup.displayTime(time.minutes)
            blueGroup.show()
          case Routes.GREEN =>
            greenGroup.displayTime(time.minutes)
            greenGroup.show()
          case Routes.RED =>
            redGroup.displayTime(time.minutes)
            redGroup.show()
        }
      }
    }
  }

  def getStopId = stop.id

  def doSubscribe() { reminderController.subscribeReminderForStop(getStopId) }

  def doUnsubscribe() { reminderController.unsubscribeReminderForStop(getStopId) }

  def isSubscribed = reminderController.isSubscribedToStop(getStopId)

}

object DetailActivity {

  val TAG_ID = "stopId"

  def openForId(id: Int, ctx: Context) {
    val i = new Intent(ctx, classOf[DetailActivity])
    i.putExtra(TAG_ID, id)
    ctx.startActivity(i)
  }

  class ArrivalTimeViewHolder(val view: View, val disp: TextView) {

    def displayTime(minutes: Int) {
      disp.setText(s"""$minutes ${if (minutes <= 1) " minute" else " minutes"}""") }

    def hide() { view.setVisibility(View.GONE) }

    def show() { view.setVisibility(View.VISIBLE) }
  }

  class ReminderViewController(val switch: Switch,
                               val text: TextView,
                               val parent: DetailActivity)
    extends View.OnClickListener with CompoundButton.OnCheckedChangeListener
  {
    switch.setChecked(parent.isSubscribed)
    switch.setOnCheckedChangeListener(this)
    text.setOnClickListener(this)

    override def onClick(v: View) { switch.toggle() }

    override def onCheckedChanged(cb: CompoundButton, checked: Boolean) {
      if (checked) parent.doSubscribe()
      else         parent.doUnsubscribe()
    }
  }

}