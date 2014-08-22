package com.vandyapps.vandyvans

import android.app.Activity
import android.os.{Message, Handler}
import android.util.Log
import android.view.View
import android.widget.{Button, LinearLayout}

import com.google.android.gms.maps.model._
import com.google.android.gms.maps.{GoogleMap, MapView, CameraUpdateFactory}

import com.marsupial.eventhub.{AppInjection, ActorConversion}
import com.vandyapps.vandyvans.models.{Van, FloatPair, Stop, Route}
import com.vandyapps.vandyvans.services.{SyncromaticsClient, VandyVansClient, Global}

trait MapController extends ActorConversion {
  self: Activity with AppInjection[Global] =>

  import MapController._
  import VandyVansClient._
  import SyncromaticsClient._

  var currentRoute: Route = null
  var markerDict = Map.empty[Marker, Stop]

  def mapview: MapView
  def overlayBar: LinearLayout
  def blueBtn: Button
  def redBtn: Button
  def greenBtn: Button

  implicit object bridge extends Handler {
    override def handleMessage(msg: Message): Unit = msg.obj match {
      case WaypointResults(waypoints) =>
        Log.i(Global.APP_LOG_ID, s"$LOG_ID | Received Waypoints")
        Log.i(Global.APP_LOG_ID, s"$LOG_ID | $waypoints")
        handleWaypointResult(waypoints)
      case StopResults(stops) => handleStopResults(stops)
      case VanResults(vans) => handleVanResults(vans)
      case "Init" =>
        mapview.getMap.setOnInfoWindowClickListener(InfoWindowClick)
        blueBtn.onClick(routeSelected(Route.BLUE))
        redBtn.onClick(routeSelected(Route.RED))
        greenBtn.onClick(routeSelected(Route.GREEN))
        routeSelected(Route.BLUE)
      case _ =>
    }
  }

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

  def handleWaypointResult(waypoints: Seq[FloatPair]) {
    val options = new PolylineOptions()
      .color(app.getColorFor(currentRoute))
      .width(DEFAULT_WIDTH)
    for (way <- waypoints) {
      options.add(new LatLng(way.lat, way.lon))
    }
    options.add(new LatLng(waypoints.head.lat, waypoints.head.lon))
    mapview.getMap.addPolyline(options)
    Log.i(Global.APP_LOG_ID, s"$LOG_ID | Handled Waypoints")
  }

  def handleStopResults(stops: Seq[Stop]) {
    for (map <- Option(mapview.getMap);
         stop <- stops) {
      val option =
        new MarkerOptions()
          .position(new LatLng(stop.latitude, stop.longitude))
          .title(stop.name)
          .draggable(false)
      markerDict += ((map.addMarker(option), stop))
    }
    Log.i(Global.APP_LOG_ID, s"$LOG_ID | Handled Stops")
  }

  def handleVanResults(vans: Seq[Van]) {
    for (map <- Option(mapview.getMap);
         van <- vans) {
      map.addMarker(new MarkerOptions()
        .position(new LatLng(van.location.lat, van.location.lon))
        .title(s"${van.name}: ${van.percentFull}% full")
        .draggable(false)
        .flat(true)
        .icon(BitmapDescriptorFactory.fromResource(R.drawable.van_icon))
        .anchor(0.5f, 0.5f))
    }
  }

  object InfoWindowClick extends GoogleMap.OnInfoWindowClickListener {
    override def onInfoWindowClick(marker: Marker): Unit = {
      markerDict.get(marker).foreach {
        s => DetailActivity.openForId(s.id, self)
      }
    }
  }

}

object MapController {

  val DEFAULT_ZOOM = 14.5f
  val DEFAULT_WIDTH = 5
  val LOG_ID = "MapController"

}