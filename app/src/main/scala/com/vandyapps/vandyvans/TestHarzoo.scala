package com.vandyapps.vandyvans

import android.app.Activity
import android.os.Bundle
import android.widget.{LinearLayout, Button}
import com.google.android.gms.maps.{MapsInitializer, MapView}
import com.marsupial.eventhub.AppInjection
import com.marsupial.eventhub.Helpers.EasyActivity
import com.vandyapps.vandyvans.services.Global

class TestHarzoo extends Activity
    with EasyActivity
    with AppInjection[Global]
    with MapController
{
  override def mapview = component[MapView](R.id.mapview)
  override def overlayBar = component[LinearLayout](R.id.linear1)
  override def redBtn = component[Button](R.id.btn_red)
  override def greenBtn = component[Button](R.id.btn_green)
  override def blueBtn = component[Button](R.id.btn_blue)

  override def onCreate(bundle: Bundle) {
    super.onCreate(bundle)
    setContentView(R.layout.fragment_map)
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

}

