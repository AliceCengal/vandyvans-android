package com.vandyapps.vandyvans.models

/** The prediction of when a van of a particular route will arrive at a stop.
  *
  * @param stop The stop that the arrival time pertains to
  * @param route The route
  * @param minutes how many more minutes before a van arrives to the stop.
  */
case class ArrivalTime(stop: Stop, route: Route, minutes: Int)

