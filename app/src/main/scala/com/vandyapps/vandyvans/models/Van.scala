package com.vandyapps.vandyvans.models

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
}