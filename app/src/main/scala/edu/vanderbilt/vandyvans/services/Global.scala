package edu.vanderbilt.vandyvans.services

import java.io.OutputStreamWriter
import java.net.URL

import android.os.{Handler, HandlerThread}
import com.parse.Parse

import com.marsupial.eventhub.{Initialize, EventfulApp}
import edu.vanderbilt.vandyvans.R
import edu.vanderbilt.vandyvans.models.{Routes, Route}

class Global extends android.app.Application with EventfulApp with Clients {

  lazy val reminders = new SimpleReminderController(this)
  lazy val serviceThread = new HandlerThread("BackgroundThread")

  lazy val vandyVans = new Handler(serviceThread.getLooper, new VandyVansClient)
  lazy val syncromatics = new Handler(serviceThread.getLooper, new SyncromaticsClient)

  def getColorFor(route: Route) = route match {
    case Routes.BLUE => getResources.getColor(R.color.blue_argb)
    case Routes.RED => getResources.getColor(R.color.red_argb)
    case Routes.GREEN => getResources.getColor(R.color.green_argb)
    case _ => getResources.getColor(android.R.color.black)
  }

  override def onCreate() {
    super.onCreate()
    serviceThread.start()
    reminders.start()

    List(vandyVans, syncromatics).foreach { _ ! Initialize(this) }

    Parse.initialize(this,
      "6XOkxBODp8HZANJaxFhEfSFPZ8H93Pt9531Htt1X",
      "61wOewMMN0YISmX3UM79PGssnTsz1NfkOOMOsHMm")

  }

}

object Global {
  val DEFAULT_LONGITUDE = -86.805811
  val DEFAULT_LATITUDE = 36.143905
  val APP_LOG_ID = "VandyVans"
  val APP_PREFERENCES = "VandyVansPreferences"

  def get(url: String) = new URL(url).openStream()

  def post(url: String, params: String) = {
    val conn = new URL(url).openConnection()
    conn.setDoInput(true)
    conn.setDoOutput(true)
    conn.setUseCaches(false)

    val writer = new OutputStreamWriter(conn.getOutputStream)
    writer.write(params)
    writer.flush()

    conn.getInputStream
  }
}