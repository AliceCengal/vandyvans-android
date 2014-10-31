package com.vandyapps.vandyvans.view

import android.app.Activity
import android.widget.{Button, ViewAnimator}
import com.cengallut.appinjection.AppInjection
import com.vandyapps.vandyvans.R
import com.vandyapps.vandyvans.services.Global

/**
 * Define the behavior of the whole overlay over the map. When the list button is clicked, slide
 * down to reveal the List page. When map button is clicked, slide up to reveal the Map page.
 *
 * Created by athran on 8/22/14.
 */
trait OverlayController {
  self: Activity with AppInjection[Global] =>

  import OverlayController._

  def pager: ViewAnimator
  def mapBtn: Button
  def listBtn: Button

  /**
   * Slide the overlay to reveal the list of stops.
   */
  def gotoList() {
    pager.setInAnimation(self, R.anim.slide_in_top)
    pager.setOutAnimation(self, R.anim.slide_out_bottom)
    pager.setDisplayedChild(1)
    app.eventHub.send(ListMode)
  }

  /**
   * Slide the overlay to reveal the map.
   */
  def gotoMap() {
    pager.setInAnimation(self, R.anim.slide_in_bottom) // dirty?
    pager.setOutAnimation(self, R.anim.slide_out_top)
    pager.setDisplayedChild(0)
    app.eventHub.send(MapMode)
  }

  uiHandler.postNow {
    mapBtn.onClick(gotoMap())
    listBtn.onClick(gotoList())
  }

}

object OverlayController {

  /** A signal object to indicate that the overlay has transitioned to Map */
  case object MapMode

  /** A signal object to indicate that the overlay has transitioned to List */
  case object ListMode

}