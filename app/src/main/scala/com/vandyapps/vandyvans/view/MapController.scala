package com.vandyapps.vandyvans.view

import android.app.Activity
import android.os.{Handler, Message}
import android.util.Log
import android.widget.{Button, LinearLayout}
import com.google.android.gms.maps.model._
import com.google.android.gms.maps.{GoogleMap, MapView}
import com.marsupial.eventhub.{EventHub, ActorConversion, AppInjection}
import com.vandyapps.vandyvans.R
import com.vandyapps.vandyvans.models.{FloatPair, Route, Stop, Van}
import com.vandyapps.vandyvans.services.Global

/**
 * Define behavior for the MapView and the bar overlay.
 */
trait MapController extends ActorConversion {
  self: Activity with AppInjection[Global] =>

  import com.vandyapps.vandyvans.services.VandyVansClient._
  import com.vandyapps.vandyvans.services.SyncromaticsClient._
  import com.vandyapps.vandyvans.view.MapController._

  private var currentRoute: Route = null
  private var markerDict = Map.empty[Marker, Stop]
  private var isLiveMapping = false

  private var waypointsData = List.empty[FloatPair]
  private var stopsData = List.empty[Stop]
  private var vansData = List.empty[Van]

  def mapview: MapView
  def overlayBar: LinearLayout
  def blueBtn: Button
  def redBtn: Button
  def greenBtn: Button

  private implicit object bridge extends Handler {
    override def handleMessage(msg: Message): Unit = msg.obj match {
      case WaypointResults(waypoints) =>
        waypointsData = waypoints
        draw()

      case StopResults(stops) =>
        stopsData = stops
        draw()

      case VanResults(vans) =>
        Log.i(Global.APP_LOG_ID, LOG_ID + " | Received Van location")
        vansData = vans
        draw()
        if (isLiveMapping)
          bridge.postDelayed(() => app.syncromatics ? FetchVans(currentRoute) , 5000)

      case "Init" =>
        mapview.getMap.setOnInfoWindowClickListener(InfoWindowClick)
        blueBtn.onClick(routeSelected(Route.BLUE))
        redBtn.onClick(routeSelected(Route.RED))
        greenBtn.onClick(routeSelected(Route.GREEN))
        routeSelected(Route.BLUE)
        app.eventHub ? EventHub.Subscribe

      case StartLiveMap => startLiveMapping()
      case StopLiveMap => stopLiveMapping()

      case _ =>
    }
  }

  bridge ! "Init"

  def startLiveMapping() {
    if (!isLiveMapping) {
      isLiveMapping = true
      app.syncromatics ? FetchVans(currentRoute)
    }
  }

  def stopLiveMapping() {
    isLiveMapping = false
  }

  def routeSelected(route: Route) {
    if (currentRoute != route) {
      currentRoute = route
      overlayBar.setBackgroundColor(app.getColorFor(currentRoute))

      // Requesting data from the services.
      app.vandyVans ? FetchWaypoints(route)
      app.vandyVans ? FetchStops(route)
      app.syncromatics ? FetchVans(route)
    }
  }

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

  private def draw() {
    for (map <- Option(mapview.getMap)) {
      map.clear()
      // Draw waypoints
      val options = new PolylineOptions()
        .color(app.getColorFor(currentRoute))
        .width(DEFAULT_WIDTH)
      for (way <- waypointsData) {
        options.add(new LatLng(way.lat, way.lon))
      }
      options.add(new LatLng(waypointsData.head.lat, waypointsData.head.lon))
      map.addPolyline(options)

      // Draw stops
      for (s <- stopsData) {
        val option =
          new MarkerOptions()
            .position(new LatLng(s.latitude, s.longitude))
            .title(s.name)
            .draggable(false)
        markerDict += ((map.addMarker(option), s))
      }

      // Draw Vans
      for (van <- vansData) {
        map.addMarker(new MarkerOptions()
          .position(new LatLng(van.location.lat, van.location.lon))
          .title(s"${van.name}: ${van.percentFull}% full")
          .draggable(false)
          .flat(true)
          .icon(BitmapDescriptorFactory.fromResource(R.drawable.van_icon))
          .anchor(0.5f, 0.5f))
      }

    }
  }

  private object InfoWindowClick extends GoogleMap.OnInfoWindowClickListener {
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

  case object StartLiveMap
  case object StopLiveMap

}