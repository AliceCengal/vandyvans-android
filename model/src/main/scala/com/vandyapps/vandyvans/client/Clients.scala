package com.vandyapps.vandyvans.client

import java.io.Reader
import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import com.google.gson.JsonParser
import com.squareup.okhttp.{Request, OkHttpClient}
import com.vandyapps.vandyvans.models._

/** An abstract declaration of the API calls provided by the Vandy Vans server.
  *
  * The companion object provides an implementation of some of these calls. All methods in this
  * trait return a [[Future]] of the result. They may or may not be actual asynchronous calls.
  * Depends on implementation.
  */
trait VansServerCalls {

  /** The thread pool executor for the Futures. */
  implicit def exec: ExecutionContext

  /** Returns the [[Van]] that runs through the given [[Route]]. The [[Van]] instances contain
    * real-time GPS data of each van as well as their occupancy level. */
  def vans(route: Route): Future[List[Van]]

  /** Returns [[ArrivalTime]] predictions for a given [[Stop]] in minutes. */
  def arrivalTimes(stop: Stop): Future[List[ArrivalTime]]

  /** Returns the [[Stop]]s visited by the given [[Route]]. */
  def stops(route: Route): Future[List[Stop]]

  /** Returns all [[Stop]]s of all [[Route]]s. */
  def stopsForAllRoutes(): Future[List[Stop]]

  /** Returns the [[Stop]] with the given ID. Returns [[scala.util.Failure]] if there is no Stop
    * with the given ID. */
  def stopsWithId(id: Int): Future[Stop]

  /** Returns the coordinates that traces the path of a [[Route]]. */
  def waypoints(route: Route): Future[List[(Double,Double)]]

  /** Posts the user [[Report]] to some server somewhere in the cloud. */
  def postReport(report: Report): Future[Unit]

}

object VansServerCalls {

  /** Returns a partial implementation of [[VansServerCalls]].
    *
    * The implemented methods are:
    *
    *  - [[VansServerCalls.vans()]]
    *  - [[VansServerCalls.arrivalTimes()]]
    *  - [[VansServerCalls.stops()]]
    *  - [[VansServerCalls.waypoints()]]
    *
    * The other methods return Null Objects.
    */
  def create(implicit exec: ExecutionContext): VansServerCalls = new VansClient

}

/** A partial implementation of [[VansServerCalls]]. Only the methods which has a direct
  * equivalent in the actual Vandy Vans API are implemented. Each method call will do a server
  * call. This class is stateless. No caching is done.
  */
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

