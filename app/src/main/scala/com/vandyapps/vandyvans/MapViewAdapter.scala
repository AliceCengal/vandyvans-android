package com.vandyapps.vandyvans

import android.app.{Fragment, Activity}
import android.os.Bundle
import com.google.android.gms.maps.MapView

trait MapViewAdapter extends Activity {

  def mapViews: Iterable[MapView]

  override def onCreate(saved: Bundle) {
    super.onCreate(saved)
    mapViews.foreach { _.onCreate(saved) }
  }

  override def onResume() {
    super.onResume()
    mapViews.foreach { _.onResume() }
  }

  override def onPause() {
    super.onPause()
    mapViews.foreach { _.onPause() }
  }

  override def onDestroy() {
    super.onDestroy()
    mapViews.foreach { _.onDestroy() }
  }

  override def onSaveInstanceState(state: Bundle) {
    super.onSaveInstanceState(state)
    mapViews.foreach { _.onSaveInstanceState(state) }
  }

  override def onLowMemory() {
    super.onLowMemory()
    mapViews.foreach { _.onLowMemory() }
  }

}


trait MapViewFragmentAdapter extends Fragment {

  def mapViews: Iterable[MapView]

  override def onResume() {
    super.onResume()
    mapViews.foreach { mv =>
      // The view hierarchy of a Fragment is not created until Fragment::onCreateView
      mv.onCreate(new Bundle)
      mv.onResume()
    }
  }

  override def onPause() {
    super.onPause()
    mapViews.foreach { _.onPause() }
  }

  override def onDestroy() {
    super.onDestroy()
    mapViews.foreach { _.onDestroy() }
  }

  override def onSaveInstanceState(state: Bundle) {
    super.onSaveInstanceState(state)
    mapViews.foreach { _.onSaveInstanceState(state) }
  }

  override def onLowMemory() {
    super.onLowMemory()
    mapViews.foreach { _.onLowMemory() }
  }

}