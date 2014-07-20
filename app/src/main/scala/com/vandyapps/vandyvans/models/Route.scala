package com.vandyapps.vandyvans.models

case class Route(id: Int, name: String)

object Route {
  val BLUE  = Route(745, "Blue")
  val RED   = Route(746, "Red")
  val GREEN = Route(749, "Green")

  def getAll = List(BLUE, RED, GREEN)

  def getForId(id: Int) = getAll.find(_.id == id).get
}