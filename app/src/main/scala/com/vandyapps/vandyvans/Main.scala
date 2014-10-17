package com.vandyapps.vandyvans

import android.app.Activity
import android.os.{Message, Handler, Bundle}
import android.view.{MenuItem, Menu}
import android.widget._
import com.cengallut.appinjection.AppInjection
import com.cengallut.asyncactivity.AsyncActivity
import com.google.android.gms.maps.{MapsInitializer, MapView}
import com.vandyapps.vandyvans.services.Global
import com.vandyapps.vandyvans.view._

class Main extends Activity
    with Handler.Callback
    with AppInjection[Global]
    with MapController
    with OverlayController
    with StopsController
    with AsyncActivity
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
    (new Handler).postNow(startLiveMapping())
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
