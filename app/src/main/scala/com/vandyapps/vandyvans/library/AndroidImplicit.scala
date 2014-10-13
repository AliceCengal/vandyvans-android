package com.vandyapps.vandyvans.library

import android.os.{Message, Handler}
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

  implicit class ControlAddon[T](target: T) {

    def forward[U](func: T=>U): U = func(target)

    def returnAfter(proc: =>Unit): T = { proc; target }

  }

  def handler(c: PartialFunction[AnyRef, Unit]): Handler = {
    new Handler() {
      override def handleMessage(msg: Message): Unit = {
        if (c.isDefinedAt(msg.obj)) c(msg.obj)
      }
    }
  }


}