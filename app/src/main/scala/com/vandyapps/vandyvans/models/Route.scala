package com.vandyapps.vandyvans.models

case class Route(id: Int, name: String, waypointId: Int)

object Route {
  val BLUE  = Route(1857, "Black", 1290) // Blue 745
  val RED   = Route(1858, "Red", 1291)   // 746
  val GREEN = Route(1856, "Gold", 1289)  // Green 749

  def getAll = List(BLUE, RED, GREEN)

  def getForId(id: Int) = getAll.find(_.id == id).getOrElse(BLUE)
}