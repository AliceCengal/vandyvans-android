package com.vandyapps.vandyvans

import android.app.Activity
import android.os.{Message, Handler}
import android.view.View
import android.view.View.OnClickListener
import android.widget.{Button, LinearLayout}

import com.google.android.gms.maps.model.{PolylineOptions, BitmapDescriptorFactory, MarkerOptions, LatLng}
import com.google.android.gms.maps.{MapView, CameraUpdateFactory}

import com.marsupial.eventhub.{AppInjection, ActorConversion}
import com.vandyapps.vandyvans.models.Route
import com.vandyapps.vandyvans.services.{SyncromaticsClient, VandyVansClient, Global}

trait MapController extends ActorConversion {
  self: Activity with AppInjection[Global] =>

  import MapController._
  import VandyVansClient._
  import SyncromaticsClient._

  def mapview: MapView
  def overlayBar: LinearLayout
  def blueBtn: Button
  def redBtn: Button
  def greenBtn: Button

  implicit lazy val bridge: Handler = new Handler() {
    override def handleMessage(msg: Message): Unit = msg.obj match {
      case m: WaypointResults => handleWaypointResult(m)
      case m: StopResults => handleStopResults(m)
      case m: VanResults => handleVanResults(m)
      case "Init" =>
        blueBtn.onClick { _ => routeSelected(Route.BLUE) }
        redBtn.onClick { _ => routeSelected(Route.RED) }
        greenBtn.onClick { _ => routeSelected(Route.GREEN) }
    }
  }

  var currentRoute = Route.BLUE

  lazy val defaultCamera =
    CameraUpdateFactory.newLatLngZoom(
      new LatLng(Global.DEFAULT_LATITUDE, Global.DEFAULT_LONGITUDE),
      DEFAULT_ZOOM)

  bridge ! "Init"

  def routeSelected(route: Route) {
    if (currentRoute != route) {
      currentRoute = route
      overlayBar.setBackgroundColor(app.getColorFor(currentRoute))

      // Requesting data from the services.
      app.vandyVans ? FetchWaypoints(route)
      app.vandyVans ? FetchStops(route)
      app.syncromatics ? FetchVans(route)

      Option(mapview.getMap).foreach { map =>
        map.clear()
      }
    }
  }

  def showOverlay(): Unit = overlayBar.setVisibility(View.VISIBLE)

  def hideOverlay(): Unit = overlayBar.setVisibility(View.GONE)

  def mapIsShown(): Unit = routeSelected(currentRoute)

  def handleWaypointResult(results: WaypointResults) {
    for (map <- Option(mapview.getMap);
         way <- results.waypoints) {
      map.addPolyline(new PolylineOptions()
        .color(app.getColorFor(currentRoute))
        .width(DEFAULT_WIDTH))
    }
  }

  def handleStopResults(results: StopResults) {
    for (map <- Option(mapview.getMap);
         stop <- results.stops) {
      map.addMarker(new MarkerOptions()
        .position(new LatLng(stop.latitude, stop.longitude))
        .title(stop.name)
        .draggable(false))
    }
  }

  def handleVanResults(results: VanResults) {
    for (map <- Option(mapview.getMap);
         van <- results.vans) {
      map.addMarker(new MarkerOptions()
        .position(new LatLng(van.location.lat, van.location.lon))
        .title(s"${van.percentFull}%")
        .draggable(false)
        .flat(true)
        .icon(BitmapDescriptorFactory.fromResource(R.drawable.van_icon))
        .anchor(0.5f, 0.5f))
    }
  }

}

object MapController {

  val DEFAULT_ZOOM = 14.5f
  val DEFAULT_WIDTH = 5
  val LOG_ID = "MapController"

}