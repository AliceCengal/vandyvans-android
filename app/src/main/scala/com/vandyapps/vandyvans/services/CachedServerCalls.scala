package com.vandyapps.vandyvans.services

import android.content.Context
import com.vandyapps.vandyvans.client.VansServerCalls
import com.vandyapps.vandyvans.models._

import scala.concurrent.{Future, ExecutionContext}
import scala.util.Success

/**
 * A decorator that adds caching to the stateless VansServerCalls. Also implements some calls
 * which is No-Op in VansServerCalls.
 *
 * Created by athran on 10/26/14.
 */
private[services]
class CachedServerCalls(ctx: Context)(implicit val exec: ExecutionContext)
    extends VansServerCalls {

  val client       = VansServerCalls.create
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
    Future.successful {
      allStops.valuesIterator.flatten
        .find(_.id == id)
        .get // Failure if not found
    }

  override def waypoints(route: Route): Future[List[(Double, Double)]] =
    allWaypoints.get(route).map(Future.successful)
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
    Future.successful { allStops.values.flatten.toSet.toList }

  override def postReport(report: Report): Future[Unit] =
    Future {
      parseClient.postReportUsingParseApi(report)
    }
}
