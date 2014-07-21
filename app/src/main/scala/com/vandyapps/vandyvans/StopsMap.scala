package com.vandyapps.vandyvans

import android.app.Fragment
import android.os.{Message, Handler, Bundle}
import android.view.View
import android.widget.{Button, LinearLayout}
import com.google.android.gms.maps.model.{BitmapDescriptorFactory, MarkerOptions, PolylineOptions, LatLng}
import com.google.android.gms.maps.{CameraUpdateFactory, MapView}

import com.marsupial.eventhub.{AppInjection, ActorConversion}
import com.marsupial.eventhub.Helpers.EasyFragment
import com.vandyapps.vandyvans.models.Route
import com.vandyapps.vandyvans.services.{SyncromaticsClient, VandyVansClient, Global}

class StopsMap extends Fragment
    with EasyFragment
    with ActorConversion
    with Handler.Callback
    with View.OnClickListener
    with MapViewFragmentAdapter
    with AppInjection[Global]
{
  import StopsMap._
  import VandyVansClient._
  import SyncromaticsClient._

  def mapView     = component[MapView](R.id.mapview)
  def overlayBar  = component[LinearLayout](R.id.linear1)
  def blueButton  = component[Button](R.id.btn_blue)
  def redButton   = component[Button](R.id.btn_red)
  def greenButton = component[Button](R.id.btn_green)

  var currentRoute = Route.BLUE
  implicit lazy val bridge = new Handler(this)

  lazy val defaultCamera =
    CameraUpdateFactory.newLatLngZoom(
      new LatLng(Global.DEFAULT_LATITUDE, Global.DEFAULT_LONGITUDE),
      DEFAULT_ZOOM)

  override def mapViews: Iterable[MapView] = Option(getView).flatMap { _ => Option(mapView) }

  override def layoutId: Int = R.layout.fragment_map

  override def onActivityCreated(saved: Bundle) {
    super.onActivityCreated(saved)
    List(blueButton, redButton, greenButton).foreach { _.setOnClickListener(this) }
  }

  override def onResume() {
    super.onResume()
    Option(mapView.getMap).foreach { _. }
  }

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
    overlayBar.setBackgroundColor(app.getColorFor(currentRoute))

    // Requesting data from the services.
    app.vandyVans ? FetchWaypoints(route)
    app.vandyVans ? FetchStops(route)
    app.syncromatics ? FetchVans(route)

    Option(mapView.getMap).foreach { map =>
      map.clear()
      map.animateCamera(defaultCamera)
      map.setMyLocationEnabled(true)
    }

  }

  override def onClick(view: View) {
    val select = view match {
      case blueButton if currentRoute != Route.BLUE => Route.BLUE
      case redButton if currentRoute != Route.RED => Route.RED
      case greenButton if currentRoute != Route.GREEN => Route.GREEN
    }
    routeSelected(select)
  }

  def showOverlay(): Unit = overlayBar.setVisibility(View.VISIBLE)

  def hideOverlay(): Unit = overlayBar.setVisibility(View.GONE)

  def mapIsShown(): Unit = routeSelected(currentRoute)

  def handleWaypointResult(results: WaypointResults) {
    for (map <- Option(mapView.getMap);
         way <- results.waypoints) {
      map.addPolyline(new PolylineOptions()
        .color(app.getColorFor(currentRoute))
        .width(DEFAULT_WIDTH))
    }
  }

  def handleStopResults(results: StopResults) {
    for (map <- Option(mapView.getMap);
         stop <- results.stops) {
      map.addMarker(new MarkerOptions()
        .position(new LatLng(stop.latitude, stop.longitude))
        .title(stop.name)
        .draggable(false))
    }
  }

  def handleVanResults(results: VanResults) {
    for (map <- Option(mapView.getMap);
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

object StopsMap {

  val DEFAULT_ZOOM = 14.5f
  val DEFAULT_WIDTH = 5
  val LOG_ID = "StopsMap"

}