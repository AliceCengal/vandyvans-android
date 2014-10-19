package com.vandyapps.vandyvans.models

import com.google.gson.JsonObject

case class Van(id: Int,
               name: String,
               percentFull: Int,
               location: FloatPair)

object Van {
  val TAG_ID = "ID"
  val TAG_NAME = "Name"
  val TAG_PERCENT_FULL = "APCPercentage"
  val TAG_LATS = "Latitude"
  val TAG_LOND = "Longitude"

  def fromJson(obj: JsonObject) =
    Van(
      obj.get(Van.TAG_ID).getAsInt,
      obj.get(Van.TAG_NAME).getAsString,
      obj.get(Van.TAG_PERCENT_FULL).getAsInt,
      new FloatPair(
        obj.get(Van.TAG_LATS).getAsDouble,
        obj.get(Van.TAG_LOND).getAsDouble))

}