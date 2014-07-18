package edu.vanderbilt.vandyvans

import scala.collection.JavaConversions._

import android.os.{Message, Handler}
import android.view.View
import android.widget.{Button, LinearLayout}

import com.google.android.gms.maps.model.{PolylineOptions, BitmapDescriptorFactory, MarkerOptions, LatLng}
import com.google.android.gms.maps.{CameraUpdateFactory, SupportMapFragment}

import edu.vanderbilt.vandyvans.models.{Route, Routes}
import edu.vanderbilt.vandyvans.services.{SyncromaticsClient, VandyVansClient, Global, Clients}

class MapController(val mapFrag: SupportMapFragment, val overlayBar: LinearLayout,
                    val blueBtn: Button, val redBtn: Button, val greenBtn: Button,
                    val clients: Clients, val global: Global)
  extends Handler.Callback with View.OnClickListener
{
  import MapController._
  import VandyVansClient._
  import SyncromaticsClient._

  lazy val bridge = new Handler(this)
  var currentRoute = Routes.BLUE

  lazy val defaultCamera =
    CameraUpdateFactory.newLatLngZoom(
      new LatLng(Global.DEFAULT_LATITUDE, Global.DEFAULT_LONGITUDE),
      DEFAULT_ZOOM)

  List(blueBtn, redBtn, greenBtn).foreach { _.setOnClickListener(this) }

  override def handleMessage(msg: Message) = {
    msg.obj match {
      case m: WaypointResults => handleWaypointResult(m)
      case m: StopResults => handleStopResults(m)
      case m: VanResults => handleVanResults(m)
    }
    true
  }

  def routeSelected(route: Route) {
    currentRoute = route
    overlayBar.setBackgroundColor(global.getColorFor(currentRoute))

    // Requesting data from the services.
    Message.obtain(clients.vandyVans(), 0, new FetchWaypoints(bridge, route))
      .sendToTarget()

    Message.obtain(clients.vandyVans(), 0, new FetchStops(bridge, route))
      .sendToTarget()

    Message.obtain(clients.syncromatics(), 0, new FetchVans(bridge, route))
      .sendToTarget()

    val map = mMapFragment.getMap()
    if (map == null) { return; }

    map.clear()
    map.animateCamera(getDefaultCameraUpdate())
    map.setMyLocationEnabled(true)
  }

  override def onClick(view: View) {
    val select = view match {
      case blueButton if currentRoute != Routes.BLUE => Routes.BLUE
      case redButton if currentRoute != Routes.RED => Routes.RED
      case greenButton if currentRoute != Routes.GREEN => Routes.GREEN
    }
    routeSelected(select)
  }

  def showOverlay(): Unit = overlayBar.setVisibility(View.VISIBLE)

  def hideOverlay(): Unit = overlayBar.setVisibility(View.GONE)

  def mapIsShown(): Unit = routeSelected(currentRoute)

  def handleWaypointResult(results: WaypointResults) {
    for (map <- Option(mapFrag.getMap);
         way <- results.waypoints) {
      map.addPolyline(new PolylineOptions()
        .color(global.getColorFor(currentRoute))
        .width(DEFAULT_WIDTH))
    }
  }

  def handleStopResults(results: StopResults) {
    for (map <- Option(mapFrag.getMap);
         stop <- results.stops) {
      map.addMarker(new MarkerOptions()
        .position(new LatLng(stop.latitude, stop.longitude))
        .title(stop.name)
        .draggable(false))
    }
  }

  def handleVanResults(results: VanResults) {
    for (map <- Option(mapFrag.getMap);
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

  def getDefaultCameraUpdate = defaultCamera

}

object MapController {

  val DEFAULT_ZOOM = 14.5f
  val DEFAULT_WIDTH = 5
  val LOG_ID = "MapController"

}