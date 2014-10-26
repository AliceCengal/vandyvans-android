package com.vandyapps.vandyvans.services

import java.io.StringWriter
import scala.collection.JavaConversions._
import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Failure, Success}

import android.content.{SharedPreferences, Context}
import android.os.{AsyncTask, Handler}
import android.util.Log

import com.google.gson.JsonParser
import com.google.gson.stream.JsonWriter
import com.parse.{ParseObject, Parse}

import com.cengallut.handlerextension.{HandlerExtensionPackage, MessageHub}
import com.vandyapps.vandyvans.client.VansServerCalls
import com.vandyapps.vandyvans.R
import com.vandyapps.vandyvans.models._

class Global extends android.app.Application
    with ReminderController
    with HandlerExtensionPackage
{
  private implicit val executionContext =
    ExecutionContext.fromExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

  private lazy val reminders = new SimpleReminderController(this)
  private lazy val servicesHolder = new CachedServerCalls
  lazy val eventHub = MessageHub.create

  private lazy val prefs = getSharedPreferences(Global.APP_PREFERENCES, Context.MODE_PRIVATE)
  private lazy val cacheManager = new DataCache(prefs)

  override def onCreate() {
    super.onCreate()
    reminders.start()

    Parse.initialize(this,
      "6XOkxBODp8HZANJaxFhEfSFPZ8H93Pt9531Htt1X",
      "61wOewMMN0YISmX3UM79PGssnTsz1NfkOOMOsHMm")

    cacheInstatement()
  }

  def getColorFor(route: Route) = route match {
    case Route.BLUE => getResources.getColor(R.color.dusky_gray)
    case Route.RED => getResources.getColor(R.color.red_argb)
    case Route.GREEN => getResources.getColor(R.color.dark_gold)
    case _ => getResources.getColor(android.R.color.black)
  }

  def subscribeReminderForStop(stopdId: Int): Unit =
    reminders.subscribeReminderForStop(stopdId)

  def unsubscribeReminderForStop(stopId: Int): Unit =
    reminders.unsubscribeReminderForStop(stopId)

  def isSubscribedToStop(stopId: Int): Boolean =
    reminders.isSubscribedToStop(stopId)

  def services: VansServerCalls = servicesHolder

  def preferences: SharedPreferences = prefs

  private def cacheInstatement(): Unit = {
    val mainThread = new Handler

    if (cacheManager.hasCache && !cacheManager.isCacheExpired) {
      val f = Future {
        val data = new JsonParser().parse(cacheManager.retrieveCache).getAsJsonObject

        Route.getAll.map { route =>
          val stops = data.get(route.name + "Stop").getAsJsonArray
            .toList
            .map(j => Stop.fromJson(j.getAsJsonObject))

          val points = data.get(route.name + "Points").getAsJsonArray
            .toList
            .map(ps => ps.getAsString.split(","))
            .map(nums => (nums(0).toDouble, nums(1).toDouble))

          (route, stops, points)
        }
      }

      f.onComplete {
        case Success(result) =>
          mainThread.postNow {
            result.foreach {
              case (r, ss, ps) =>
                servicesHolder.allStops += r -> ss
                servicesHolder.allWaypoints += r -> ps
            }
          }
        case Failure(ex) =>
      }

    } else {
      val redStops   = servicesHolder.stops(Route.RED)
      val greenStops = servicesHolder.stops(Route.GREEN)
      val blueStops  = servicesHolder.stops(Route.BLUE)
      val redPath    = servicesHolder.waypoints(Route.RED)
      val greenPath  = servicesHolder.waypoints(Route.GREEN)
      val bluePath   = servicesHolder.waypoints(Route.BLUE)

      for {
        rs <- redStops
        gs <- greenStops
        bs <- blueStops
        rp <- redPath
        gp <- greenPath
        bp <- bluePath
      } {

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

        mainThread.postNow {
          Log.d(Global.APP_LOG_ID, "retrieved data from server")
          servicesHolder.allStops =
            Map(Route.RED -> rs, Route.GREEN -> gs, Route.BLUE -> bs)
          servicesHolder.allWaypoints =
            Map(Route.RED -> rp, Route.GREEN -> gp, Route.BLUE -> bp)
          cacheManager.cacheData(string.toString)
        }
      }
    }
  }

}

object Global {
  val DEFAULT_LONGITUDE = -86.805811
  val DEFAULT_LATITUDE = 36.143905
  val APP_LOG_ID = "VandyVans"
  val APP_PREFERENCES = "VandyVansPreferences"
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

private[services] class ParseClient {
  import ParseClient._

  def postReportUsingParseApi(report: Report) {
    val reportObj = new ParseObject(REPORT_CLASSNAME)
    reportObj.put(REPORT_USEREMAIL, report.senderAddress)
    reportObj.put(REPORT_BODY     , report.bodyOfReport)
    reportObj.put(REPORT_ISBUG    , report.isBugReport)
    reportObj.put(REPORT_NOTIFY   , report.notifyWhenResolved)
    reportObj.save()
  }

}

private[services] object ParseClient {
  private val REPORT_CLASSNAME = "VVReport"
  private val REPORT_USEREMAIL = "userEmail"
  private val REPORT_BODY      = "body"
  private val REPORT_ISBUG     = "isBugReport"
  private val REPORT_NOTIFY    = "notifyWhenResolved"
}

private[services]
class CachedServerCalls(implicit val exec: ExecutionContext)
    extends VansServerCalls {

  val client       = VansServerCalls.create
  val parseClient  = new ParseClient
  val mainThread   = uiHandler
  var allStops     = Map.empty[Route, List[Stop]]
  var allWaypoints = Map.empty[Route, List[(Double,Double)]]

  override def vans(route: Route): Future[List[Van]] =
    client.vans(route)

  override def stops(route: Route): Future[List[Stop]] =
    allStops.get(route).map(Future(_))
      .getOrElse({
      client.stops(route).andThen { case Success(ss) =>
        mainThread.postNow {
          allStops += (route -> ss)
        }
      }
    })

  override def stopsWithId(id: Int): Future[Stop] =
    Future {
      allStops.valuesIterator.flatten
        .find(_.id == id)
        .getOrElse(Stop(0, ""))
    }

  override def waypoints(route: Route): Future[List[(Double, Double)]] =
    allWaypoints.get(route).map(Future(_))
      .getOrElse({
      client.waypoints(route).andThen { case Success(ws) =>
        mainThread.postNow {
          allWaypoints += (route -> ws)
        }
      }
    })

  override def arrivalTimes(stop: Stop): Future[List[ArrivalTime]] =
    client.arrivalTimes(stop)

  override def stopsForAllRoutes(): Future[List[Stop]] =
    Future { allStops.values.flatten.toSet.toList }

  override def postReport(report: Report): Future[Unit] =
    Future {
      parseClient.postReportUsingParseApi(report)
    }
}
