package com.vandyapps.vandyvans.client

import java.io.Reader
import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import com.google.gson.JsonParser
import com.squareup.okhttp.{Request, OkHttpClient}
import com.vandyapps.vandyvans.models._

trait VansServerCalls {

  implicit def exec: ExecutionContext

  def vans(route: Route): Future[List[Van]]

  def arrivalTimes(stop: Stop): Future[List[ArrivalTime]]

  def stops(route: Route): Future[List[Stop]]

  def stopsForAllRoutes(): Future[List[Stop]]

  def stopsWithId(id: Int): Future[Stop]

  def waypoints(route: Route): Future[List[(Double,Double)]]

  def postReport(report: Report): Future[Unit]

}

object VansServerCalls {

  def create(implicit exec: ExecutionContext): VansServerCalls = new VansClient

}

private[client]
class VansClient(implicit val exec: ExecutionContext) extends VansServerCalls {

  lazy val client  = new OkHttpClient
  lazy val parser  = new JsonParser
  lazy val request = new Request.Builder

  override def vans(route: Route): Future[List[Van]] =
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

  override def stops(route: Route): Future[List[Stop]] =
    Future {
      val stream =
        fetchAsStream(VansClient.stopsFetchUrl(route))
      val stopsResult =
        parser.parse(stream).getAsJsonArray.toList
          .map(elem => Stop.fromJson(elem.getAsJsonObject))
      stream.close()
      stopsResult
    }

  override def stopsForAllRoutes(): Future[List[Stop]] =
    Future.successful(List.empty[Stop])

  override def stopsWithId(id: Int): Future[Stop] =
    Future.successful(Stop(0, "No Stop"))

  override def waypoints(route: Route): Future[List[(Double, Double)]] =
    Future {
      val stream =
        fetchAsStream(VansClient.waypointsFetchUrl(route))
      val pointsResult =
        parser.parse(stream).getAsJsonArray.get(0).getAsJsonArray.toList
          .map(_.getAsJsonObject)
          .map(FloatPair.asPair _ compose FloatPair.fromJson)
      stream.close()
      pointsResult
    }

  override def arrivalTimes(stop: Stop): Future[List[ArrivalTime]] =
    Future.successful(Route.getAll).flatMap { routes =>
      val fs = routes.map(arrivalTime(stop, _))

      fs.foldLeft(Future(List.empty[ArrivalTime])) {
        (fats, fat) =>
          (for {
            ats <- fats
            at <- fat
          } yield at :: ats)
            // This is totally unintentional, I swear.
            .recoverWith { case NonFatal(e) => fats } }
    }

  def arrivalTime(stop: Stop, r: Route): Future[ArrivalTime] =
    Future {
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
    }

  override def postReport(report: Report): Future[Unit] =
    Future.successful({})

  def fetchAsStream(url: String): Reader =
    client.newCall(request.url(url).build()).execute().body().charStream()

}

private[client] object VansClient {

  private val SYN_BASE_URL = "http://api.syncromatics.com"
  private val SYN_API_KEY  = "?api_key=a922a34dfb5e63ba549adbb259518909"
  private val VV_BASE_URL  = "http://vandyvans.com"

  def vanFetchUrl(route: Route) =
    s"$SYN_BASE_URL/Route/${route.waypointId}/Vehicles$SYN_API_KEY"

  def stopsFetchUrl(route: Route) =
    s"$VV_BASE_URL/Route/${route.id}/Direction/0/Stops"

  def arrivalFetchUrl(stop: Stop, route: Route) =
    s"$SYN_BASE_URL/Route/${route.id}/Stop/${stop.id}/Arrivals$SYN_API_KEY"

  def waypointsFetchUrl(route: Route) =
    s"$VV_BASE_URL/Route/${route.waypointId}/Waypoints"

}

