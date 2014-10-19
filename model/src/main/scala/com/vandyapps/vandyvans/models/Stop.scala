package com.vandyapps.vandyvans.models

import com.google.gson.JsonObject
import com.google.gson.stream.JsonWriter

case class Stop(id: Int,
                name: String,
                image: String = "",
                latitude: Double = 0.0,
                longitude: Double = 0.0,
                rtpi: Int = -1)
{
  import Stop._

  def writeJson(writer: JsonWriter): Unit = {
    writer.beginObject()
      .name(TAG_ID).value(id)
      .name(TAG_IMAGE).value(image)
      .name(TAG_LAT).value(latitude)
      .name(TAG_LON).value(longitude)
      .name(TAG_NAME).value(name)
      .name(TAG_RTPI).value(rtpi)
      .endObject()
  }
}

object Stop {
  val TAG_ID    = "ID"
  val TAG_IMAGE = "Image"
  val TAG_LAT   = "Latitude"
  val TAG_LON   = "Longitude"
  val TAG_NAME  = "Name"
  val TAG_RTPI  = "RtpiNumber"

  def forId(id: Int): Option[Stop] = staticList.find(_.id == id)

  def getShortList = staticList.toList.take(4)

  def getAll = staticList.toList

  private val staticList =
    Array(
      Stop(263473, "Branscomb Quad"),
      Stop(263470, "Carmichael Tower"),
      Stop(263454, "Murray House"),
      Stop(263444, "Highland Quad"),
      Stop(264041, "Vanderbilt Police Department"),
      Stop(332298, "Vanderbilt Book Store"),
      Stop(263415, "Kissam Quad"),
      Stop(238083, "Terrace Place Garage"),
      Stop(238096, "Wesley Place Garage"),
      Stop(263463, "North House"),
      Stop(264091, "Blair School of Music"),
      Stop(264101, "McGugin Center"),
      Stop(401204, "Blakemore House"),
      Stop(446923, "Medical Center"))

  def fromJson(jsonObj: JsonObject) =
    Stop(
      id = jsonObj.get(Stop.TAG_ID).getAsInt,
      name = jsonObj.get(Stop.TAG_NAME).getAsString,
      image = jsonObj.get(Stop.TAG_IMAGE).getAsString,
      latitude = jsonObj.get(Stop.TAG_LAT).getAsDouble,
      longitude = jsonObj.get(Stop.TAG_LON).getAsDouble)



}