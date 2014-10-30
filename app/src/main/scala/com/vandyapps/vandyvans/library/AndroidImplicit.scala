package com.vandyapps.vandyvans.library

import android.app.Activity
import android.view.View
import android.view.View.OnClickListener

trait AndroidImplicit {

  implicit class RichView(v: View) {

    def component[T <: View](id: Int) =
      v.findViewById(id).asInstanceOf[T]

    def onClick(handler: =>Unit) {
      v.setOnClickListener(new OnClickListener {
        override def onClick(v: View): Unit = { handler }
      })
    }
  }

  implicit def funToRunnable(fun: () => Unit) = new Runnable() { def run() = fun() }

  implicit class ViewAccess(a: Activity) {

    def component[V](id: Int): V = a.findViewById(id).asInstanceOf[V]

  }

}