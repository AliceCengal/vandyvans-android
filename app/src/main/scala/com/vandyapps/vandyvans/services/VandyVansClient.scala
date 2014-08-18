package com.vandyapps.vandyvans.services

import scala.collection.JavaConversions._
import scala.util.control.NonFatal
import scala.io.Source

import android.content.{Context, SharedPreferences}
import android.util.Log
import android.os.Handler
import com.google.gson.{JsonObject, JsonElement, JsonParser}
import com.parse.ParseObject

import com.marsupial.eventhub.{Initialize, Server}
import com.vandyapps.vandyvans.models._

object VandyVansClient {

  case class FetchStops(route: Route)
  case class FetchWaypoints(route: Route)
  case class StopResults(stops: List[Stop])
  case class WaypointResults(waypoints: List[FloatPair])

  private val LOG_TAG    = "VandyVansClient"
  private val BASE_URL   = "http://vandyvans.com"
  private val REPORT_URL = "http://studentorgs.vanderbilt.edu/vandymobile/bugReport.php"
  private val PARSER     = new JsonParser

  private val REPORT_CLASSNAME = "VVReport"
  private val REPORT_USEREMAIL = "userEmail"
  private val REPORT_BODY      = "body"
  private val REPORT_ISBUG     = "isBugReport"
  private val REPORT_NOTIFY    = "notifyWhenResolved"

  private val ROUTE_CACHE_DATE       = "VandyVansClientRouteCacheDate"
  private val ROUTE_DATA             = "VandyVansClientRouteData"
  private val STOPS_CACHE_DATE       = "VandyVansClientStopsCacheDate"
  private val STOPS_DATA             = "VandyVansClientStopsData"
  private val CACHE_EXPIRATION: Long = 14 * 24 * 3600 * 1000

  def extractStop(json: JsonElement): List[Stop] = {

    def buildFromJson(jsonObj: JsonObject): Stop =
      Stop(id = jsonObj.get(Stop.TAG_ID).getAsInt,
           name = jsonObj.get(Stop.TAG_NAME).getAsString,
           image = jsonObj.get(Stop.TAG_IMAGE).getAsString,
           latitude = jsonObj.get(Stop.TAG_LAT).getAsDouble,
           longitude = jsonObj.get(Stop.TAG_LON).getAsDouble)

    json match {
      case j if j.isJsonArray =>
        j.getAsJsonArray.flatMap {
          case elem if elem.isJsonObject =>
            Some(buildFromJson(elem.getAsJsonObject))
          case _ => None }
          .toList
      case j if j.isJsonObject =>
        List(buildFromJson(j.getAsJsonObject))
      case _ => List.empty[Stop]
    }
  }

  def extractWaypoints(json: JsonElement): List[FloatPair] = {
    json.getAsJsonArray.get(0).getAsJsonArray
      .map { _.getAsJsonObject }
      .map { obj =>
        new FloatPair(
          obj.get(FloatPair.TAG_LAT).getAsDouble,
          obj.get(FloatPair.TAG_LON).getAsDouble) }
      .toList
  }

}

private[services] class VandyVansClient extends Handler.Callback with Server {
  import VandyVansClient._

  private var prefs: SharedPreferences = null

  override def handleRequest(msg: AnyRef) = msg match {
    case Initialize(ctx) =>
      prefs = ctx.getSharedPreferences(Global.APP_PREFERENCES, Context.MODE_PRIVATE)

    case FetchStops(route) =>
      val cacheId = STOPS_DATA + route.id
      val dateCacheId = STOPS_CACHE_DATE + route.id

      def doHttpFetch() {
        val requestUrl = s"$BASE_URL/Route/${route.id}/Direction/0/Stops"
        Log.i(Global.APP_LOG_ID, s"$LOG_TAG | Getting Stops with $requestUrl")
        try {
          val raw = Source.fromInputStream(Global.get(requestUrl)).mkString
          val result = extractStop(PARSER.parse(raw))
          Log.i(Global.APP_LOG_ID, s"$LOG_TAG | Received Stops")
          Log.i(Global.APP_LOG_ID, s"$LOG_TAG | $result")
          requester ! StopResults(result)
          storeCacheRawDataUpdateDate(cacheId, raw, dateCacheId)
        } catch {
          case NonFatal(e) =>
            Log.e(Global.APP_LOG_ID, LOG_TAG + " | Failed to get Stops for Route.")
            Log.e(Global.APP_LOG_ID, LOG_TAG + " | URL: " + requestUrl)
            Log.e(Global.APP_LOG_ID, e.getMessage)
        }
      }

      if (prefs.contains(cacheId) && false) {
        if (!isCacheExpired(dateCacheId)) {
          val result = extractStop(PARSER.parse(prefs.getString(cacheId, "")))
          if (result.isEmpty) {
            invalidateCache(cacheId)
            doHttpFetch()
          } else {
            requester ! StopResults(result)
          }
        } else {
          invalidateCache(cacheId)
          doHttpFetch()
        }
      } else {
        doHttpFetch()
      }

    case FetchWaypoints(route) =>
      val cacheId = ROUTE_DATA + route.id
      val dateCacheId = ROUTE_CACHE_DATE + route.id

      def doHttpFetch() {
        val requestUrl = s"$BASE_URL/Route/${route.waypointId}/Waypoints"
        Log.i(Global.APP_LOG_ID, s"$LOG_TAG | Getting Waypoints with $requestUrl")
        try {
          val raw = Source.fromInputStream(Global.get(requestUrl)).mkString
          val result = extractWaypoints(PARSER.parse(raw))
          if (result.isEmpty) {
            Log.e(Global.APP_LOG_ID, LOG_TAG + " | Failed to get Waypoints for Route.")
            Log.e(Global.APP_LOG_ID, LOG_TAG + " | URL: " + requestUrl)
            Log.e(Global.APP_LOG_ID, s"$LOG_TAG | $raw")
          } else {
            Log.i(Global.APP_LOG_ID, s"$LOG_TAG | Received Waypoints")
            Log.i(Global.APP_LOG_ID, s"$LOG_TAG | $result")
          }
          requester ! WaypointResults(result)
          storeCacheRawDataUpdateDate(cacheId, raw, dateCacheId)
        } catch {
          case NonFatal(e) =>
            Log.e(Global.APP_LOG_ID, LOG_TAG + " | Failed to get Waypoints for Route.")
            Log.e(Global.APP_LOG_ID, LOG_TAG + " | URL: " + requestUrl)
            Log.e(Global.APP_LOG_ID, e.getMessage)
        }
      }

      if (prefs.contains(cacheId) && false) {
        if (!isCacheExpired(dateCacheId)) {
          val result = extractWaypoints(PARSER.parse(prefs.getString(cacheId, "")))
          if (result.isEmpty) {
            invalidateCache(cacheId)
            doHttpFetch()
          } else {
            requester ! result
          }
        } else {
          invalidateCache(cacheId)
          doHttpFetch()
        }
      } else {
        doHttpFetch()
      }

    case r: Report => postReportUsingParseApi(r)

    case _ =>
  }

  private def postReportUsingParseApi(report: Report) {
    val reportObj = new ParseObject(REPORT_CLASSNAME)
    reportObj.put(REPORT_USEREMAIL, report.senderAddress)
    reportObj.put(REPORT_BODY     , report.bodyOfReport)
    reportObj.put(REPORT_ISBUG    , report.isBugReport)
    reportObj.put(REPORT_NOTIFY   , report.notifyWhenResolved)
    reportObj.saveEventually()
  }

  private def isCacheExpired(cacheDateId: String) = {
    val currentTime = System.currentTimeMillis()
    val cacheDate = prefs.getLong(cacheDateId, currentTime)
    (currentTime - cacheDate) > CACHE_EXPIRATION
  }

  private def invalidateCache(cacheId: String) {
    prefs.edit.remove(cacheId).apply()
  }

  private def storeCacheRawDataUpdateDate(cacheId: String, data: String, cacheDateId: String) {
    prefs.edit()
      .putString(cacheId, data)
      .putLong(cacheDateId, System.currentTimeMillis())
      .apply()
  }

}