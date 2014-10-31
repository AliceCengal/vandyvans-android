package com.vandyapps.vandyvans

import android.app.Activity
import android.os.Bundle
import android.view.{MenuItem, Menu}
import android.widget._
import com.google.android.gms.maps.{MapsInitializer, MapView}
import com.cengallut.appinjection.AppInjection
import com.cengallut.asyncactivity.AsyncActivity
import com.cengallut.handlerextension.MessageHub
import com.vandyapps.vandyvans.services.Global
import com.vandyapps.vandyvans.view._

/** The principal [[Activity]] of the app. Shows the main page, and contains the core functions.
  *
  * This class is fairly simple. All the functionalities are broken off into supporting traits.
  * The main job of this class is to satiate the beast that is Google Maps API V2. It is
  * horrendously picky, and any attempt to subvert it will result in a crash. This class
  * represents several months of my effort chasing down Exceptions and researching StackOverflow
  * threads for non-sensical solutions to non-sensical problems. Leave it as it is. Don't touch.
  *
  * layout: R.layout.activity_stop
  *
  * ===Mixins===
  * [[AppInjection]] is provided by app-injection.aar and is documented in [[https://github
  * .com/AliceCengal/android-app-injection the Github page]].
  *
  * [[AsyncActivity]] is provided by async-activity.aar and is documented in [[https://github
  * .com/AliceCengal/android-async-activity the Github page]].
  *
  * [[MapController]] provides operations related to the MapView itself.
  *
  * [[OverlayController]] controls the transition of the overlay.
  *
  * [[StopsController]] populates the ListView and handles click.
  */
class Main extends Activity
    with AppInjection[Global]
    with AsyncActivity
    with MapController
    with OverlayController
    with StopsController
{
  override def mapview    = this.component[MapView](R.id.mapview)
  override def overlayBar = this.component[LinearLayout](R.id.linear1)
  override def redBtn     = this.component[Button](R.id.btn_red)
  override def greenBtn   = this.component[Button](R.id.btn_green)
  override def blueBtn    = this.component[Button](R.id.btn_blue)

  override def pager    = this.component[ViewAnimator](R.id.pager)
  override def listBtn  = this.component[Button](R.id.btn_list)
  override def mapBtn   = this.component[Button](R.id.btn_map)
  override def stopList = this.component[ListView](R.id.listView1)

  lazy val bridge = uiHandler {
    case OverlayController.ListMode =>
      stopLiveMapping()
      app.preferences.edit().putInt("InitialScreen", 0).apply()
    case OverlayController.MapMode  =>
      startLiveMapping()
      app.preferences.edit().putInt("InitialScreen", 1).apply()
  }

  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    setContentView(R.layout.activity_stop)
    mapview.onCreate(bundle)
    MapsInitializer.initialize(this)
  }

  override def onResume() {
    super.onResume()
    mapview.onResume()
    app.eventHub.send(MessageHub.Subscribe(bridge))
    bridge.postNow(startLiveMapping())
  }

  override def onPause() {
    super.onPause()
    mapview.onPause()
    stopLiveMapping()
    app.eventHub.send(MessageHub.Unsubscribe(bridge))
  }

  override def onDestroy() {
    super.onDestroy()
    mapview.onDestroy()
  }

  override def onSaveInstanceState(state: Bundle) {
    super.onSaveInstanceState(state)
    mapview.onSaveInstanceState(state)
  }

  override def onLowMemory() {
    super.onLowMemory()
    mapview.onLowMemory()
  }

  override def onCreateOptionsMenu(menu: Menu) = {
    getMenuInflater.inflate(R.menu.stop, menu); true
  }

  override def onOptionsItemSelected(item: MenuItem) = {
    item.getItemId match {
      case R.id.action_settings => AboutsActivity.open(this); true
      case _ => super.onOptionsItemSelected(item)
    }
  }

}
