package com.vandyapps.vandyvans

import android.app.Activity
import android.os.{Message, Handler, Bundle}
import android.view.{View, MenuItem, Menu}
import android.widget._
import com.google.android.gms.maps.{MapsInitializer, MapView}
import com.marsupial.eventhub.{ActorConversion, AppInjection}
import com.marsupial.eventhub.Helpers.EasyActivity
import com.vandyapps.vandyvans.models.{Route, Stop}
import com.vandyapps.vandyvans.services.{VandyVansClient, Global}

class Main extends Activity
    with EasyActivity
    with AppInjection[Global]
    with MapController
    with OverlayController
    with StopsController
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

  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    setContentView(R.layout.activity_stop)
    mapview.onCreate(bundle)
    MapsInitializer.initialize(this)
  }

  override def onResume() {
    super.onResume()
    mapview.onResume()
  }

  override def onPause() {
    super.onPause()
    mapview.onPause()
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
    if (item.getItemId == R.id.action_settings) {
      AboutsActivity.open(this); true
    } else super.onOptionsItemSelected(item)
  }

}

trait OverlayController extends ActorConversion {
  self: Activity with AppInjection[Global] =>

  def pager: ViewAnimator
  def mapBtn: Button
  def listBtn: Button

  private[OverlayController] implicit object handler extends Handler {
    override def handleMessage(msg: Message): Unit = {
      msg.obj match {
        case "init" =>
          mapBtn.onClick(gotoMap())
          listBtn.onClick(gotoList())
        case _ =>
      }
    }
  }

  def gotoList() {
    pager.setInAnimation(self, R.anim.slide_in_top)
    pager.setOutAnimation(self, R.anim.slide_out_bottom)
    pager.setDisplayedChild(1)
  }

  def gotoMap() {
    pager.setInAnimation(self, R.anim.slide_in_bottom) // dirty?
    pager.setOutAnimation(self, R.anim.slide_out_top)
    pager.setDisplayedChild(0)
  }

  handler ! "init"
}

trait StopsController extends ActorConversion {
  self: Activity with AppInjection[Global] =>

  import VandyVansClient._

  def stopList: ListView

  private[StopsController] implicit object handler extends Handler {
    override def handleMessage(msg: Message): Unit = {
      msg.obj match {
        case "init" =>
          app.vandyVans ? FetchAllStops

        case VandyVansClient.StopResults(stops) =>
          stopList.setAdapter(ArrayAdapterBuilder
            .fromCollection(stops.toArray)
            .withContext(self)
            .withResource(R.layout.simple_text)
            .withStringer(StopToString)
            .build())
          stopList.setOnItemClickListener(StopItemClick)
        case _ =>
      }
    }
  }

  private object StopToString extends ArrayAdapterBuilder.ToString[Stop] {
    override def apply(s: Stop) = s.name
  }

  private object StopItemClick extends AdapterView.OnItemClickListener {
    override def onItemClick(parent: AdapterView[_],
                             view: View,
                             position: Int,
                             id: Long): Unit = {
      DetailActivity.openForId(
        parent.getItemAtPosition(position).asInstanceOf[Stop].id,
        self)
    }
  }

  handler ! "init"
}