package com.vandyapps.vandyvans.library

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

}