package edu.vanderbilt.vandyvans.services

import java.io.InputStreamReader
import scala.collection.JavaConversions._
import scala.util.control.NonFatal

import android.os.Handler
import android.util.Log
import com.google.gson.JsonParser

import com.marsupial.eventhub.{Server, Initialize}
import edu.vanderbilt.vandyvans.models._

object SyncromaticsClient {

  case class FetchVans(route: Route)
  case class FetchArrivalTimes(stop: Stop)
  case class VanResults(vans: List[Van])
  case class ArrivalTimeResults(times: List[Van])

  private val LOG_TAG  = "SyncromaticsClient"
  private val BASE_URL = "http://api.syncromatics.com"
  private val API_KEY  = "?api_key=a922a34dfb5e63ba549adbb259518909"
  private val PARSER   = new JsonParser

}

private[services] class SyncromaticsClient
  extends Handler.Callback with Server
{
  import SyncromaticsClient._

  override def handleRequest(msg: AnyRef) = msg match {
    case Initialize(ctx) =>
    case FetchVans(route) =>
      val requestUrl = s"$BASE_URL/Route/${route.id}/Vehicles$API_KEY"

      try {
        val reader = new InputStreamReader(Global.get(requestUrl))
        val result =
          PARSER.parse(reader).getAsJsonArray
            .map { elem.getAsJsonObject }
            .map { obj =>
              new Van(
                obj.get(Van.TAG_ID).getAsInt,
                obj.get(Van.TAG_PERCENT_FULL).getAsInt,
                new FloatPair(
                  obj.get(Van.TAG_LATS).getAsDouble,
                  obj.get(Van.TAG_LOND).getAsDouble)) }

        requester ! VanResults(result)
      } catch {
        case NonFatal(e) =>
          Log.e(Global.APP_LOG_ID, s"$LOG_TAG | Failed to get Vans for Route.")
          Log.e(Global.APP_LOG_ID, s"$LOG_TAG | URL: $requestUrl")
          Log.e(Global.APP_LOG_ID, e.getMessage)
      } finally {
        reader.close()
      }

    case FetchArrivalTimes(stop) =>

      def readArrivalTime(route: Route): Option[ArrivalTime] = {
        val requestUrl = s"$BASE_URL/Route/${route.id}/Stop/${stop.id}/Arrivals$API_KEY"

        try {
          val reader = new InputStreamReader(Global.get(requestUrl))
          val responseObj = PARSER.parse(reader).getAsJsonObject
          val predictionObj =
            responseObj
              .get("Predictions").getAsJsonArray
              .get(0).getAsJsonObject
          reader.close()
          Some(new ArrivalTime(stop, route, predictionObj.get("Minutes").getAsInt))
        } catch {
          case NonFatal(e) => None
        }
      }

      val results = Routes.getAll.flatMap { readArrivalTime }
      requester ! ArrivalTimeResults(results)

    case _ =>
  }

}

