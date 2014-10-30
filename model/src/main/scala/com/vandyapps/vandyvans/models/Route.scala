package com.vandyapps.vandyvans.models

/** A circuit around the campus serviced by shuttle vans.
  *
  * As of October 2014, there are three routes in service. This could (and has) change.
  *
  * I '''do''' not have access to the official API of Vandy Vans. The identification numbers below
  * are obtained through dark magic, and a pinch of goat blood. Contact Athran if you want to know.
  *
  * @param id The number used by the VV system to identify this route
  * @param name The name of this route, in the fashion of colors
  * @param waypointId The secondary id used by the VV system
  */
case class Route(id: Int, name: String, waypointId: Int)

object Route {

  /** A route. */
  val BLUE  = Route(1857, "Black", 1290) // Blue 745

  /** A route. */
  val RED   = Route(1858, "Red", 1291)   // 746

  /** A route. */
  val GREEN = Route(1856, "Gold", 1289)  // Green 749

  /** Returns a list of all the routes. */
  def getAll = List(BLUE, RED, GREEN)

  /** Returns a route which has the given id. TODO make it return Option[Route]. */
  def getForId(id: Int): Route =
    getAll.find(_.id == id).getOrElse(BLUE)
}