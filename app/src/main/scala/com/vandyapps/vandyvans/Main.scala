package com.vandyapps.vandyvans

import android.app.Activity
import android.os.{Message, Handler, Bundle}
import android.view.{MenuItem, Menu}
import android.widget._
import com.cengallut.asyncactivity.AsyncActivity
import com.google.android.gms.maps.{MapsInitializer, MapView}
import com.marsupial.eventhub.{ChattyActivity, AppInjection}
import com.marsupial.eventhub.Helpers.EasyActivity
import com.vandyapps.vandyvans.services.Global
import com.vandyapps.vandyvans.view._

class Main extends Activity
    with EasyActivity
    with ChattyActivity
    with Handler.Callback
    with AppInjection[Global]
    with MapController
    with OverlayController
    with StopsController
    with AsyncActivity
{
  override def mapview    = component[MapView](R.id.mapview)
  override def overlayBar = component[LinearLayout](R.id.linear1)
  override def redBtn     = component[Button](R.id.btn_red)
  override def greenBtn   = component[Button](R.id.btn_green)
  override def blueBtn    = component[Button](R.id.btn_blue)

  override def pager    = component[ViewAnimator](R.id.pager)
  override def listBtn  = component[Button](R.id.btn_list)
  override def mapBtn   = component[Button](R.id.btn_map)
  override def stopList = component[ListView](R.id.listView1)

  override def handleMessage(msg: Message): Boolean = {
    msg.obj match {
      case OverlayController.ListMode => stopLiveMapping()
      case OverlayController.MapMode => startLiveMapping()
      case _ =>
    }
    true
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
    communicator.post(() => startLiveMapping())
  }

  override def onPause() {
    super.onPause()
    mapview.onPause()
    stopLiveMapping()
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

  override def onBackPressed() {
    if (pager.getDisplayedChild == 0) {
      super.onBackPressed()
    } else {
      gotoMap()
    }
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
