package com.vandyapps.vandyvans.view

import android.app.Activity
import android.os.{Handler, Message}
import android.view.View
import android.widget.{AdapterView, ListView}
import com.cengallut.asyncactivity.AsyncActivity
import com.marsupial.eventhub.{ActorConversion, AppInjection}
import com.vandyapps.vandyvans.library.ArrayAdapterBuilder
import com.vandyapps.vandyvans.models.Stop
import com.vandyapps.vandyvans.services.{Global, VandyVansClient}

import scala.util.Success

/**
 * Defines the behavior of the list of stop in Main Activity. Pull the list of Stops from Services.
 * When ListView item is clicked, go to detail page.
 *
 * Created by athran on 8/22/14.
 */
trait StopsController extends ActorConversion {
  self: Activity with AppInjection[Global] with AsyncActivity =>

  def stopList: ListView

  private[StopsController] val bridge = handler {
    case "init" =>
      app.services.stopsForAllRoutes().onCompleteForUi {
        case Success(ss) =>
          stopList.setAdapter(ArrayAdapterBuilder
            .fromCollection(ss.toArray)
            .withContext(self)
            .withResource(android.R.layout.simple_list_item_1)
            .withStringer(StopToString)
            .build())
          stopList.setOnItemClickListener(StopItemClick)
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

  bridge ! "init"
}
