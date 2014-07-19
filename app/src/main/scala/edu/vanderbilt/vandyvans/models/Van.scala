package edu.vanderbilt.vandyvans.models

case class Van(id: Int, percentFull: Int, location: FloatPair)

object Van {
  val TAG_ID = "ID"
  val TAG_PERCENT_FULL = "APCPercentage"
  val TAG_LATS = "Latitude"
  val TAG_LOND = "Longitude"
}