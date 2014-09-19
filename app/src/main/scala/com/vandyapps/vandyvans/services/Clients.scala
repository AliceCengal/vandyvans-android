package com.vandyapps.vandyvans.services

import scala.concurrent.{Future, ExecutionContext}
import android.os.Handler
import com.vandyapps.vandyvans.models.{ArrivalTime, Stop, Route, Van}

trait Clients {
  def vandyVans: Handler
  def syncromatics: Handler
}

trait VansServerCalls {

  def vans(route: Route)
          (implicit exec: ExecutionContext): Future[List[Van]]

  def arrivalTimes(stop: Stop)
                  (implicit exec: ExecutionContext): Future[List[ArrivalTime]]

  def stops(route: Route)(implicit exec: ExecutionContext): Future[List[Stop]]

  def stopsWithId(id: Int)(implicit exec: ExecutionContext): Future[Stop]

  def waypoints(route: Route)
               (implicit exec: ExecutionContext): Future[List[(Double,Double)]]

}





