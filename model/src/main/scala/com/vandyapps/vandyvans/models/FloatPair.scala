package com.vandyapps.vandyvans.models

import com.google.gson.JsonObject

/** A pair of real number, used to represent coordinates. This is actually a remnant of the Java
  * version of this app. It's obsolete now since Scala has native support for tuples. Someone
  * should comb through the codebase and try to remove all usages of this class.
  *
  * @param lat the left member of the pair, usually represents latitude
  * @param lon the right member of the pair, usually represents longitude
  */
case class FloatPair(lat: Double, lon: Double)

object FloatPair {

  /** The JSON tag used by the VV API for latitude. */
  val TAG_LAT = "Latitude"

  /** The JSON tag used by the VV API for longitude. */
  val TAG_LON = "Longitude"

  /** Creates a [[FloatPair]] object from a JsonObject. */
  def fromJson(obj: JsonObject) =
    FloatPair(
      obj.get(TAG_LAT).getAsDouble,
      obj.get(TAG_LON).getAsDouble)

  /** Transform a [[FloatPair]] object into a native Scala tuple. */
  def asPair(fp: FloatPair) = (fp.lat, fp.lon)

}