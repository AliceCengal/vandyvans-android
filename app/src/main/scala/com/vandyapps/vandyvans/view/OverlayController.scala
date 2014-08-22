package com.vandyapps.vandyvans.view

import android.app.Activity
import android.os.{Handler, Message}
import android.widget.{Button, ViewAnimator}
import com.marsupial.eventhub.{ActorConversion, AppInjection}
import com.vandyapps.vandyvans.R
import com.vandyapps.vandyvans.services.Global

/**
 * Define the behavior of the whole overlay over the map. When the list button is clicked, slide
 * down to reveal the List page. When map button is clicked, slide up to reveal the Map page.
 *
 * Created by athran on 8/22/14.
 */
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
