package com.vandyapps.vandyvans

import android.app.Activity
import android.content.{Intent, Context}
import android.os.{Message, Bundle, Handler}
import android.view.View
import android.widget._

import com.marsupial.eventhub.Helpers.EasyActivity
import com.marsupial.eventhub.{ChattyActivity, AppInjection}
import com.vandyapps.vandyvans.models.{Route, ArrivalTime, Stop}
import com.vandyapps.vandyvans.services.Global
import com.vandyapps.vandyvans.services.SyncromaticsClient._
import com.vandyapps.vandyvans.services.VandyVansClient._

class DetailActivity extends Activity
    with EasyActivity
    with Handler.Callback
    with AppInjection[Global]
    with ChattyActivity
{
  import DetailActivity._

  def blueRL    = component[RelativeLayout](R.id.rl1)
  def blueDisp  = component[TextView](R.id.tv1)
  def redRL     = component[RelativeLayout](R.id.rl2)
  def redDisp   = component[TextView](R.id.tv2)
  def greenRL   = component[RelativeLayout](R.id.rl3)
  def greenDisp = component[TextView](R.id.tv3)

  lazy val blueGroup  = new ArrivalTimeViewHolder(blueRL, blueDisp)
  lazy val redGroup   = new ArrivalTimeViewHolder(redRL, redDisp)
  lazy val greenGroup = new ArrivalTimeViewHolder(greenRL, greenDisp)

  def arrivalLoading = component[ProgressBar](R.id.progress1)
  def failureText    = component[TextView](R.id.tv4)
  def reminderText   = component[TextView](R.id.tv5)
  def reminderSwitch = component[Switch](R.id.cb1)

  lazy val reminderViewController =
    new ReminderViewController(reminderSwitch, reminderText, this)
  var stop: Stop = null

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

    app.vandyVans ? FetchStopWith(stopId)
  }

  override def handleMessage(msg: Message) = {
    msg.obj match {
      case x: ArrivalTimeResults =>
        displayArrivalTimes(x.times)
      case StopResults(stops) =>
        for (s <- stops; ab <- Option(getActionBar)) {
          ab.setTitle(s.name)
          app.syncromatics ? FetchArrivalTimes(s)
        }
    }
    false
  }

  def displayArrivalTimes(times: Iterable[ArrivalTime]) {
    arrivalLoading.setVisibility(View.GONE)
    if (times.isEmpty) {
      failureText.setVisibility(View.VISIBLE)
    } else {
      times.foreach { time =>
        time.route match {
          case Route.BLUE =>
            blueGroup.displayTime(time.minutes)
            blueGroup.show()
          case Route.GREEN =>
            greenGroup.displayTime(time.minutes)
            greenGroup.show()
          case Route.RED =>
            redGroup.displayTime(time.minutes)
            redGroup.show()
        }
      }
    }
  }

  def getStopId = stop.id

  def doSubscribe() { app.subscribeReminderForStop(getStopId) }

  def doUnsubscribe() { app.unsubscribeReminderForStop(getStopId) }

  def isSubscribed = app.isSubscribedToStop(getStopId)

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