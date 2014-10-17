package com.vandyapps.vandyvans.view

import android.app.Activity
import android.os.{Handler, Message}
import android.util.Log
import android.widget.{Button, LinearLayout}
import com.cengallut.appinjection.AppInjection
import com.cengallut.asyncactivity.AsyncActivity
import com.google.android.gms.maps.model._
import com.google.android.gms.maps.{GoogleMap, MapView}
import com.marsupial.eventhub.{ActorConversion}
import com.vandyapps.vandyvans.R
import com.vandyapps.vandyvans.models.{Route, Stop, Van}
import com.vandyapps.vandyvans.services.Global

import scala.util.Success

/**
 * Define behavior for the MapView and the bar overlay.
 */
trait MapController extends ActorConversion {
  self: Activity with AppInjection[Global] with AsyncActivity =>

  import com.vandyapps.vandyvans.services.SyncromaticsClient._
  import com.vandyapps.vandyvans.view.MapController._

  private var currentRoute: Route = null
  private var markerDict = Map.empty[Marker, Stop]
  private var isLiveMapping = false

  private var vansData = List.empty[Van]

  def mapview: MapView
  def overlayBar: LinearLayout
  def blueBtn: Button
  def redBtn: Button
  def greenBtn: Button

  private implicit object bridge extends Handler {
    override def handleMessage(msg: Message): Unit = msg.obj match {
      case VanResults(vans) =>
        Log.i(Global.APP_LOG_ID, LOG_ID + " | Received Van location")
        vansData = vans
        draw()

        bridge.postDelayed(() =>
          if (isLiveMapping)
            app.syncromatics ? FetchVans(currentRoute) ,
          5000)

      case "Init" =>
        mapview.getMap.setOnInfoWindowClickListener(InfoWindowClick)
        blueBtn.onClick(routeSelected(Route.BLUE))
        redBtn.onClick(routeSelected(Route.RED))
        greenBtn.onClick(routeSelected(Route.GREEN))
        routeSelected(Route.BLUE)
        //app.eventHub ? EventHub.Subscribe

      case StartLiveMap => startLiveMapping()
      case StopLiveMap => stopLiveMapping()

      case _ =>
    }
  }

  bridge ! "Init"

  def startLiveMapping() {
    if (!isLiveMapping) {
      isLiveMapping = true
      //app.syncromatics ? FetchVans(currentRoute)
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
      app.services.waypoints(route).onCompleteForUi {
        case Success(ws) => drawWaypoints(mapview.getMap, ws) }

      app.services.stops(route).onCompleteForUi {
        case Success(ss) => drawStops(mapview.getMap, ss) }

      app.services.vans(route).onCompleteForUi {
        case Success(vs) =>
      }

      app.syncromatics ? FetchVans(route)
    }
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

  def drawWaypoints(map: GoogleMap, waypoints: Seq[(Double,Double)]): Unit = {
    if (waypoints.nonEmpty) {
      val options = new PolylineOptions()
        .color(app.getColorFor(currentRoute))
        .width(DEFAULT_WIDTH)
      for (way <- waypoints) {
        options.add(new LatLng(way._1, way._2))
      }
      options.add(new LatLng(waypoints.head._1, waypoints.head._2))
      map.addPolyline(options)
    }
  }

  def drawStops(map: GoogleMap, stops: Seq[Stop]): Unit = {
    for (s <- stops) {
      val option =
        new MarkerOptions()
          .position(new LatLng(s.latitude, s.longitude))
          .title(s.name)
          .draggable(false)
      markerDict += ((map.addMarker(option), s))
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