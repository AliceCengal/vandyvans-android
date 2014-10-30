package com.vandyapps.vandyvans.services

import scala.concurrent.{Future, ExecutionContext}
import scala.util.Success
import android.content.Context
import android.util.Log
import com.vandyapps.vandyvans.client.VansServerCalls
import com.vandyapps.vandyvans.models._

/**
 * A decorator that adds caching to the stateless VansServerCalls. Also implements some calls
 * which is No-Op in VansServerCalls.
 *
 * Created by athran on 10/26/14.
 */
private[services]
class CachedServerCalls(ctx: Context)(implicit val exec: ExecutionContext)
    extends VansServerCalls {

  val client       = new LogServerCall(VansServerCalls.create)
  val mainThread   = uiHandler
  var allStops     = Map.empty[Route, List[Stop]]
  var allWaypoints = Map.empty[Route, List[(Double,Double)]]

  lazy val parseClient  = new ParseClient(ctx)

  override def vans(route: Route): Future[List[Van]] =
    client.vans(route)

  override def stops(route: Route): Future[List[Stop]] =
    allStops.get(route).map(Future.successful)
      .getOrElse({
      client.stops(route).andThen { case Success(ss) =>
        mainThread.postNow {
          allStops += (route -> ss)
        }
      }
    })

  override def stopsWithId(id: Int): Future[Stop] =
    allStops.values.flatten.find(_.id == id) match {
      case Some(s) => Future.successful(s)
      case None    => Future.failed(new NoSuchElementException)
    }

  override def waypoints(route: Route): Future[List[(Double, Double)]] =
    allWaypoints.get(route) match {
      case Some(ws) => Future.successful(ws)
      case None     =>
        client.waypoints(route).andThen { case Success(ws) =>
          mainThread.postNow { allWaypoints += (route -> ws) }
        }
    }

  override def arrivalTimes(stop: Stop): Future[List[ArrivalTime]] =
    client.arrivalTimes(stop)

  override def stopsForAllRoutes(): Future[List[Stop]] = {
    val fs = Route.getAll.map(stops)
    Future.sequence(fs).map(_.flatten.distinct)
  }

  override def postReport(report: Report): Future[Unit] =
    Future {
      parseClient.postReportUsingParseApi(report)
    }
}

/**
 * A decorator that adds logging to each call to VansServerClient
 */
private[services]
class LogServerCall(val client: VansServerCalls)
                   (implicit val exec: ExecutionContext)
    extends VansServerCalls
{

  @inline private def log(msg: String): Unit = Log.i(Global.APP_LOG_ID, msg)

  override def stops(route: Route): Future[List[Stop]] = {
    log(s"Fetching stops for $route")
    client.stops(route)
  }

  override def stopsWithId(id: Int): Future[Stop] = {
    log(s"Fetching stop with id $id")
    client.stopsWithId(id)
  }

  override def waypoints(route: Route): Future[List[(Double, Double)]] = {
    log(s"Fetching waypoints for $route")
    client.waypoints(route)
  }

  override def postReport(report: Report): Future[Unit] = {
    log(s"Posting report")
    client.postReport(report)
  }

  override def vans(route: Route): Future[List[Van]] = {
    log(s"Fetching vans for $route")
    client.vans(route)
  }

  override def arrivalTimes(stop: Stop): Future[List[ArrivalTime]] = {
    log(s"Fetching arrival times for $stop")
    client.arrivalTimes(stop)
  }

  override def stopsForAllRoutes(): Future[List[Stop]] = {
    log(s"Fetching stops for all routes")
    client.stopsForAllRoutes()
  }
}
