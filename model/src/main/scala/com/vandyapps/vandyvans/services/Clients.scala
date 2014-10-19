package com.vandyapps.vandyvans.services

import java.io.Reader

import com.google.gson.JsonParser
import com.parse.ParseObject
import com.squareup.okhttp.{Request, OkHttpClient}
import com.vandyapps.vandyvans.models._

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait VansServerCalls {

  def vans(route: Route)
          (implicit exec: ExecutionContext): Future[List[Van]]

  def arrivalTimes(stop: Stop)
                  (implicit exec: ExecutionContext): Future[List[ArrivalTime]]

  def stops(route: Route)
           (implicit exec: ExecutionContext): Future[List[Stop]]

  def stopsForAllRoutes()(implicit exec: ExecutionContext): Future[List[Stop]]

  def stopsWithId(id: Int)
                 (implicit exec: ExecutionContext): Future[Stop]

  def waypoints(route: Route)
               (implicit exec: ExecutionContext): Future[List[(Double,Double)]]

  def postReport(report: Report)
                (implicit exec: ExecutionContext): Future[Unit]

}

private[services] class VansClient extends VansServerCalls {

  lazy val client  = new OkHttpClient
  lazy val parser  = new JsonParser
  lazy val request = new Request.Builder
  var allStops     = Map.empty[Route, List[Stop]]
  var allWaypoints = Map.empty[Route, List[(Double,Double)]]

  override def vans(route: Route)
                   (implicit exec: ExecutionContext): Future[List[Van]] =
    Future {
      val stream =
        fetchAsStream(VansClient.vanFetchUrl(route))
      val vanResult =
          parser.parse(stream).getAsJsonArray
            .map(elem => Van.fromJson(elem.getAsJsonObject))
            .toList
      stream.close()
      vanResult
    }

  override def stops(route: Route)
                    (implicit exec: ExecutionContext): Future[List[Stop]] =
    Future {
      allStops.getOrElse(route, {
        val stream =
          fetchAsStream(VansClient.stopsFetchUrl(route))
        val stopsResult =
          parser.parse(stream).getAsJsonArray.toList
            .map(elem => Stop.fromJson(elem.getAsJsonObject))
        allStops += (route -> stopsResult)
        stream.close()
        stopsResult
      })
    }

  override def stopsForAllRoutes()(implicit exec: ExecutionContext): Future[List[Stop]] =
    Future {
      allStops.values.flatten.toList
    }

  override def stopsWithId(id: Int)
                          (implicit exec: ExecutionContext): Future[Stop] =
    Future {
      allStops.values.flatten.find(_.id == id).get
    }

  override def waypoints(route: Route)
                        (implicit exec: ExecutionContext): Future[List[(Double, Double)]] =
    Future {
      allWaypoints.getOrElse(route, {
        val stream =
          fetchAsStream(VansClient.waypointsFetchUrl(route))
        val pointsResult =
          parser.parse(stream).getAsJsonArray.get(0).getAsJsonArray.toList
            .map(_.getAsJsonObject)
            .map(FloatPair.asPair _ compose FloatPair.fromJson)
        allWaypoints += (route -> pointsResult)
        stream.close()
        pointsResult
      })
    }

  override def arrivalTimes(stop: Stop)
                           (implicit exec: ExecutionContext): Future[List[ArrivalTime]] =
    Future {
      Route.getAll
        .map { r =>
          Try {
            val stream =
              fetchAsStream(VansClient.arrivalFetchUrl(stop, r))
            val arrivalResultObject =
              parser.parse(stream).getAsJsonObject
                .get("Predictions").getAsJsonArray
                .get(0).getAsJsonObject

            ArrivalTime(
              stop = stop,
              route = r,
              minutes = arrivalResultObject.get("Minutes").getAsInt)
          }.toOption }
        .flatten
    }

  override def postReport(report: Report)
                         (implicit exec: ExecutionContext): Future[Unit] =
    Future {
      VansClient.postReportUsingParseApi(report)
    }

  def fetchAsStream(url: String): Reader =
    client.newCall(request.url(url).build()).execute().body().charStream()

}

private[services] object VansClient {

  private val SYN_BASE_URL = "http://api.syncromatics.com"
  private val SYN_API_KEY = "?api_key=a922a34dfb5e63ba549adbb259518909"

  private val VV_BASE_URL   = "http://vandyvans.com"

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

  def vanFetchUrl(route: Route) =
    s"$SYN_BASE_URL/Route/${route.waypointId}/Vehicles$SYN_API_KEY"

  def stopsFetchUrl(route: Route) =
    s"$VV_BASE_URL/Route/${route.id}/Direction/0/Stops"

  def arrivalFetchUrl(stop: Stop, route: Route) =
    s"$SYN_BASE_URL/Route/${route.id}/Stop/${stop.id}/Arrivals$SYN_API_KEY"

  def waypointsFetchUrl(route: Route) =
    s"$VV_BASE_URL/Route/${route.waypointId}/Waypoints"

  private def postReportUsingParseApi(report: Report) {
    val reportObj = new ParseObject(REPORT_CLASSNAME)
    reportObj.put(REPORT_USEREMAIL, report.senderAddress)
    reportObj.put(REPORT_BODY     , report.bodyOfReport)
    reportObj.put(REPORT_ISBUG    , report.isBugReport)
    reportObj.put(REPORT_NOTIFY   , report.notifyWhenResolved)
    reportObj.save()
  }

}

