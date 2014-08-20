package com.vandyapps.vandyvans

import android.app.Activity
import android.os.{Message, Handler, Bundle}
import android.widget.{ViewAnimator, ListView, LinearLayout, Button}
import com.google.android.gms.maps.{MapsInitializer, MapView}
import com.marsupial.eventhub.{ActorConversion, AppInjection}
import com.marsupial.eventhub.Helpers.EasyActivity
import com.vandyapps.vandyvans.services.Global

class TestHarzoo extends Activity
    with EasyActivity
    with AppInjection[Global]
    with MapController
    with OverlayController
{
  override def mapview = component[MapView](R.id.mapview)
  override def overlayBar = component[LinearLayout](R.id.linear1)
  override def redBtn = component[Button](R.id.btn_red)
  override def greenBtn = component[Button](R.id.btn_green)
  override def blueBtn = component[Button](R.id.btn_blue)

  override def pager = component[ViewAnimator](R.id.pager)
  override def listBtn = component[Button](R.id.btn_list)
  override def mapBtn = component[Button](R.id.btn_map)
  def stopList = component[ListView](R.id.listView1)

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

}

trait OverlayController extends ActorConversion {
  self: Activity with AppInjection[Global] =>

  def pager: ViewAnimator
  def mapBtn: Button
  def listBtn: Button

  implicit object handler extends Handler {
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

  implicit object handler extends Handler {
    override def handleMessage(msg: Message): Unit = {
      case "init" =>
        
      case _ =>
    }
  }

  handler ! "init"
}