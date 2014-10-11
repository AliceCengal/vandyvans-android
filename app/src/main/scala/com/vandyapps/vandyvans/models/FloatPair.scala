package com.vandyapps.vandyvans.models

import com.google.gson.JsonObject

case class FloatPair(lat: Double, lon: Double)

object FloatPair {
  val TAG_LAT = "Latitude"
  val TAG_LON = "Longitude"

  def fromJson(obj: JsonObject) =
    FloatPair(
      obj.get(TAG_LAT).getAsDouble,
      obj.get(TAG_LON).getAsDouble)

  def asPair(fp: FloatPair) = (fp.lat, fp.lon)

}