package com.vandyapps.vandyvans.view

import android.util.Log
import com.cengallut.handlerextension.MessageHub

import scala.util.{Failure, Success}
import android.app.Activity
import android.view.View
import android.widget.{AdapterView, ListView}
import com.cengallut.appinjection.AppInjection
import com.cengallut.asyncactivity.AsyncActivity
import com.vandyapps.vandyvans.library.ArrayAdapterBuilder
import com.vandyapps.vandyvans.models.Stop
import com.vandyapps.vandyvans.services.Global

/**
 * Defines the behavior of the list of stop in Main Activity. Pull the list of Stops from Services.
 * When ListView item is clicked, go to detail page.
 *
 * Created by athran on 8/22/14.
 */
trait StopsController {
  self: Activity with AppInjection[Global] with AsyncActivity =>

  def stopList: ListView

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

  private val bridge = uiHandler {
    case OverlayController.ListMode =>
      app.services.stopsForAllRoutes().onCompleteForUi {
        case Success(ss) =>
          stopList.setAdapter(ArrayAdapterBuilder
            .fromCollection(ss.toArray)
            .withContext(self)
            .withResource(android.R.layout.simple_list_item_1)
            .withStringer(StopToString)
            .build())
          stopList.setOnItemClickListener(StopItemClick)

        case Failure(ex) =>
          Log.e(Global.APP_LOG_ID, "Failed to fetch stops for all routes")
          Log.e(Global.APP_LOG_ID, ex.getMessage)
      }
  }

  bridge.postNow {
    app.eventHub.send(MessageHub.Subscribe(bridge))
  }

}
