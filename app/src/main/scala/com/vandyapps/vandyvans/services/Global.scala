package com.vandyapps.vandyvans.services

import java.io.{StringWriter, OutputStreamWriter}
import java.net.URL
import scala.collection.JavaConversions._
import android.content.{SharedPreferences, Context}
import android.os.{AsyncTask, Handler, HandlerThread}
import android.util.Log
import com.google.gson.JsonParser
import com.google.gson.stream.JsonWriter
import com.parse.Parse

import com.marsupial.eventhub.{Initialize, EventfulApp}
import com.vandyapps.vandyvans.R
import com.vandyapps.vandyvans.models.{Stop, Route}

import scala.concurrent.ExecutionContext

class Global extends android.app.Application
                     with EventfulApp
                     with Clients
                     with ReminderController
{
  private lazy val reminders = new SimpleReminderController(this)
  private lazy val serviceThread = new HandlerThread("BackgroundThread")

  lazy val vandyVans = new Handler(serviceThread.getLooper, new VandyVansClient)
  lazy val syncromatics = new Handler(serviceThread.getLooper, new SyncromaticsClient)

  val servicesHolder = new VansClient(this)

  lazy val prefs   = getSharedPreferences(Global.APP_PREFERENCES, Context.MODE_PRIVATE)
  lazy val cacheManager = new DataCache(prefs)

  def getColorFor(route: Route) = route match {
    case Route.BLUE => getResources.getColor(R.color.dusky_gray)
    case Route.RED => getResources.getColor(R.color.red_argb)
    case Route.GREEN => getResources.getColor(R.color.dark_gold)
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

    cacheInstatement()
  }

  def subscribeReminderForStop(stopdId: Int): Unit =
    reminders.subscribeReminderForStop(stopdId)

  def unsubscribeReminderForStop(stopId: Int): Unit =
    reminders.unsubscribeReminderForStop(stopId)

  def isSubscribedToStop(stopId: Int): Boolean =
    reminders.isSubscribedToStop(stopId)

  def services: VansServerCalls = servicesHolder

  private def cacheInstatement(): Unit = {
    if (cacheManager.hasCache && !cacheManager.isCacheExpired) {
      val data = new JsonParser().parse(cacheManager.retrieveCache).getAsJsonObject

      Route.getAll.foreach { route =>
        val stops = data.get(route.name + "Stop").getAsJsonArray
          .toList
          .map(j => Stop.fromJson(j.getAsJsonObject))
        servicesHolder.allStops += route -> stops

        val points = data.get(route.name + "Points").getAsJsonArray
          .toList
          .map(ps => ps.getAsString.split(","))
          .map(nums => (nums(0).toDouble, nums(1).toDouble))
        servicesHolder.allWaypoints += route -> points
      }

    } else {
      implicit val executionContext =
        ExecutionContext.fromExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
      val redStops   = servicesHolder.stops(Route.RED)
      val greenStops = servicesHolder.stops(Route.GREEN)
      val blueStops  = servicesHolder.stops(Route.BLUE)
      val redPath    = servicesHolder.waypoints(Route.RED)
      val greenPath  = servicesHolder.waypoints(Route.GREEN)
      val bluePath   = servicesHolder.waypoints(Route.BLUE)
      val handler    = new Handler()

      for (rs <- redStops;
           gs <- greenStops;
           bs <- blueStops;
           rp <- redPath;
           gp <- greenPath;
           bp <- bluePath) {

        val string = new StringWriter()
        val jsonWriter = new JsonWriter(string)

        jsonWriter.beginObject()

        jsonWriter.name(Route.RED.name + "Stop").beginArray()
        rs.foreach { stop => stop.writeJson(jsonWriter)}
        jsonWriter.endArray()

        jsonWriter.name(Route.GREEN.name + "Stop").beginArray()
        gs.foreach { stop => stop.writeJson(jsonWriter)}
        jsonWriter.endArray()

        jsonWriter.name(Route.BLUE.name + "Stop").beginArray()
        bs.foreach { stop => stop.writeJson(jsonWriter)}
        jsonWriter.endArray()

        jsonWriter.name(Route.RED.name + "Points").beginArray()
        rp.foreach { point => jsonWriter.value(s"${point._1},${point._2}") }
        jsonWriter.endArray()

        jsonWriter.name(Route.GREEN.name + "Points").beginArray()
        rp.foreach { point => jsonWriter.value(s"${point._1},${point._2}") }
        jsonWriter.endArray()

        jsonWriter.name(Route.BLUE.name + "Points").beginArray()
        rp.foreach { point => jsonWriter.value(s"${point._1},${point._2}") }
        jsonWriter.endArray()

        jsonWriter.endObject()

        handler.post(new Runnable() {
          override def run(): Unit = {
            Log.d(Global.APP_LOG_ID, "retrieved data from server")
            servicesHolder.allStops =
              Map(Route.RED -> rs, Route.GREEN -> gs, Route.BLUE -> bs)
            servicesHolder.allWaypoints =
              Map(Route.RED -> rp, Route.GREEN -> gp, Route.BLUE -> bp)
            cacheManager.cacheData(string.toString)
          }
        })
      }
    }
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

private[services] class DataCache(prefs: SharedPreferences) {

  val GLOBAL_DATA_CACHE = "GLOBAL_DATA_CACHE"
  val GLOBAL_CACHE_TIME = "GLOBAL_CACHE_TIME"
  private val CACHE_EXPIRATION: Long = 14 * 24 * 3600 * 1000

  def hasCache =
    prefs.contains(GLOBAL_DATA_CACHE) &&
    prefs.contains(GLOBAL_CACHE_TIME)

  def isCacheExpired = {
    val currentTime = System.currentTimeMillis()
    val cacheTime = prefs.getLong(GLOBAL_CACHE_TIME, 0L)
    (currentTime - cacheTime) > CACHE_EXPIRATION
  }

  def cacheData(data: String): Unit = {
    prefs.edit()
      .putLong(GLOBAL_CACHE_TIME, System.currentTimeMillis())
      .putString(GLOBAL_DATA_CACHE, data)
      .apply()
  }

  def retrieveCache = prefs.getString(GLOBAL_DATA_CACHE, "")

}

private[services] class UserSettings(prefs: SharedPreferences) {

}
