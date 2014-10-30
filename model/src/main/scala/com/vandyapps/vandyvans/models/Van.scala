package com.vandyapps.vandyvans.models

import com.google.gson.JsonObject

/** A shuttle van.
  *
  * @param id The id used by the VV system to identify this Van
  * @param name The name of the Van
  * @param percentFull How full is the Van
  * @param location The van's location
  */
case class Van(id: Int,
               name: String,
               percentFull: Int,
               location: FloatPair)

object Van {

  /** JSON tag */
  val TAG_ID = "ID"

  /** JSON tag */
  val TAG_NAME = "Name"

  /** JSON tag */
  val TAG_PERCENT_FULL = "APCPercentage"

  /** JSON tag */
  val TAG_LATS = "Latitude"

  /** JSON tag */
  val TAG_LOND = "Longitude"

  /** Deserializes a JSON object into an instance of Van. */
  def fromJson(obj: JsonObject) =
    Van(
      obj.get(Van.TAG_ID).getAsInt,
      obj.get(Van.TAG_NAME).getAsString,
      obj.get(Van.TAG_PERCENT_FULL).getAsInt,
      new FloatPair(
        obj.get(Van.TAG_LATS).getAsDouble,
        obj.get(Van.TAG_LOND).getAsDouble))

}