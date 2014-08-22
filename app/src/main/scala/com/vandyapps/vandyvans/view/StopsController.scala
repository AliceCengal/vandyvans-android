package com.vandyapps.vandyvans.view

import android.app.Activity
import android.os.{Handler, Message}
import android.view.View
import android.widget.{AdapterView, ListView}
import com.marsupial.eventhub.{ActorConversion, AppInjection}
import com.vandyapps.vandyvans.models.Stop
import com.vandyapps.vandyvans.services.{Global, VandyVansClient}
import com.vandyapps.vandyvans.{ArrayAdapterBuilder, R}

/**
 * Defines the behavior of the list of stop in Main Activity. Pull the list of Stops from Services.
 * When ListView item is clicked, go to detail page.
 *
 * Created by athran on 8/22/14.
 */
trait StopsController extends ActorConversion {
  self: Activity with AppInjection[Global] =>

  import com.vandyapps.vandyvans.services.VandyVansClient._

  def stopList: ListView

  private[StopsController] implicit object handler extends Handler {
    override def handleMessage(msg: Message): Unit = {
      msg.obj match {
        case "init" =>
          app.vandyVans ? FetchAllStops

        case VandyVansClient.StopResults(stops) =>
          stopList.setAdapter(ArrayAdapterBuilder
            .fromCollection(stops.toArray)
            .withContext(self)
            .withResource(R.layout.simple_text)
            .withStringer(StopToString)
            .build())
          stopList.setOnItemClickListener(StopItemClick)
        case _ =>
      }
    }
  }

  private object StopToString extends ArrayAdapterBuilder.ToString[Stop] {
    override def apply(s: Stop) = s.name
  }

  private object StopItemClick extends AdapterView.OnItemClickListener {
    override def onItemClick(parent: AdapterView[_],
                             view: View,
                             position: Int,
                             id: Long): Unit = {
      DetailActivity.openForId(
        parent.getItemAtPosition(position).asInstanceOf[Stop].id,
        self)
    }
  }

  handler ! "init"
}
