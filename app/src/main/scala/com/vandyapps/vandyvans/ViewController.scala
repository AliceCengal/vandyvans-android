package com.vandyapps.vandyvans

import android.os.Handler
import android.view.View

trait ViewController {
  def handler: Handler
  def view: View
}