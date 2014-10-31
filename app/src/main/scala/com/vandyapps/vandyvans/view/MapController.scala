package com.vandyapps.vandyvans.view

import scala.util.{Failure, Success}
import android.util.Log
import android.app.Activity
import android.widget.{Button, LinearLayout}
import com.google.android.gms.maps.model._
import com.google.android.gms.maps.{GoogleMap, MapView}
import com.cengallut.handlerextension.HandlerExt
import com.cengallut.appinjection.AppInjection
import com.cengallut.asyncactivity.AsyncActivity
import com.vandyapps.vandyvans.R
import com.vandyapps.vandyvans.models.{Route, Stop, Van}
import com.vandyapps.vandyvans.services.Global

/**
 * Define behavior for the MapView and the bar overlay.
 */
trait MapController {
  self: Activity with AppInjection[Global] with AsyncActivity =>

  import MapController._

  private var currentRoute: Route = null
  private var isLiveMapping = false
  private lazy val vanTracker = new VanTracker(mapview.getMap)
  private lazy val stopTracker = new StopTracker(mapview.getMap, this)

  def mapview: MapView
  def overlayBar: LinearLayout
  def blueBtn: Button
  def redBtn: Button
  def greenBtn: Button

  private val bridge = uiHandler {
    case StartLiveMap => startLiveMapping()
    case StopLiveMap => stopLiveMapping()
    case "tock" =>
      if (isLiveMapping) {
        app.services.vans(currentRoute).onCompleteForUi {
          case Success(vs) => vanTracker.displayVans(vs)
          case Failure(ex) => }
      }
  }

  bridge.postNow {
    mapview.getMap.setOnInfoWindowClickListener(stopTracker)
    blueBtn.onClick(routeSelected(Route.BLUE))
    redBtn.onClick(routeSelected(Route.RED))
    greenBtn.onClick(routeSelected(Route.GREEN))
    routeSelected(Route.BLUE)
  }

  private val interval = 5000

  private val tick = uiHandler {
    case h: HandlerExt => h.sendDelayed(interval, h)
  }

  private val tock = uiHandler {
    case h: HandlerExt =>
      Log.i(Global.APP_LOG_ID, "Received Tick")
      bridge.send("tock")
      tick.send(h)
  }

  tick.send(tock) // ha ha ha

  def startLiveMapping() {
    isLiveMapping = true
  }

  def stopLiveMapping() {
    isLiveMapping = false
  }

  private def routeSelected(route: Route) {
    if (currentRoute != route) {
      currentRoute = route
      overlayBar.setBackgroundColor(app.getColorFor(route))

      vanTracker.purgeMarkers()
      stopTracker.purgeMarkers()
      mapview.getMap.clear()

      // Requesting data from the services.
      app.services.waypoints(route).onSuccessForUi {
        case ws =>
          drawWaypoints(mapview.getMap, app.getColorFor(route), ws) }

      app.services.stops(route).onSuccessForUi {
        case ss => stopTracker.displayStops(ss) }

      app.services.vans(route).onSuccessForUi {
        case vs => vanTracker.displayVans(vs) }

    }
  }

  private def drawWaypoints(map: GoogleMap,
                    color: Int,
                    waypoints: Seq[(Double,Double)]): Unit =
  {
    if (waypoints.nonEmpty) {
      val options = new PolylineOptions()
        .color(color)
        .width(DEFAULT_WIDTH)
      for (way <- waypoints) {
        options.add(new LatLng(way._1, way._2))
      }
      options.add(new LatLng(waypoints.head._1, waypoints.head._2))
      map.addPolyline(options)
    }
  }

}

object MapController {

  val DEFAULT_ZOOM = 14.5f
  val DEFAULT_WIDTH = 5
  val LOG_ID = "MapController"

  case object StartLiveMap
  case object StopLiveMap

  private class VanTracker(private val map: GoogleMap) {

    private var markers = List.empty[Marker]

    def purgeMarkers(): Unit = {
      markers = List.empty
    }

    def displayVans(vans: Seq[Van]): Unit = {
      if (vans.length > markers.length) {
        val extra = vans.length - markers.length
        (1 to extra).foreach { _ =>
          val option =
            new MarkerOptions()
              .position(new LatLng(Global.DEFAULT_LATITUDE, Global.DEFAULT_LONGITUDE))
              .draggable(false)
              .flat(true)
              .icon(BitmapDescriptorFactory.fromResource(R.drawable.van_icon))
              .anchor(0.5f, 0.5f)
          markers ::= map.addMarker(option)
        }
      }

      for (i <- 0 until markers.length) {
        if (i < vans.length) {
          val m = markers(i)
          val v = vans(i)
          m.setVisible(true)
          m.setPosition(new LatLng(v.location.lat, v.location.lon))
          m.setTitle(s"${v.name}: ${v.percentFull}% full")

        } else {
          markers(i).setVisible(false)
        }
      }
    }

  }

  private class StopTracker(private val map: GoogleMap, private val ctx: Activity)
      extends GoogleMap.OnInfoWindowClickListener {

    private var markers = List.empty[Marker]
    private var mStops = Seq.empty[Stop]

    override def onInfoWindowClick(m: Marker): Unit = {
      mStops.find(_.name == m.getTitle)
        .foreach { s => DetailActivity.openForId(s.id, ctx) }
    }

    def purgeMarkers(): Unit = {
      markers = List.empty
    }

    def displayStops(stops: Seq[Stop]): Unit = {
      this.mStops = stops
      if (stops.length > markers.length) {
        val extra = stops.length - markers.length
        (1 to extra).foreach { _ =>
          val option =
            new MarkerOptions()
              .position(new LatLng(Global.DEFAULT_LATITUDE, Global.DEFAULT_LONGITUDE))
              .draggable(false)
          markers ::= map.addMarker(option)
        }
      }

      for (i <- 0 until markers.length) {
        if (i < stops.length) {
          val m = markers(i)
          val s = stops(i)
          m.setVisible(true)
          m.setPosition(new LatLng(s.latitude, s.longitude))
          m.setTitle(s.name)

        } else {
          markers(i).setVisible(false)
        }
      }
    }

  }

}