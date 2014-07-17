package com.marsupial.eventhub

import android.app.{Fragment, Activity}
import android.os.Handler

trait AppInjection[APP <: EventfulApp] {

  def app = this match {
    case a: Activity => a.getApplication.asInstanceOf[APP]
    case f: Fragment => f.getActivity.getApplication.asInstanceOf[APP]
    case _           => throw new IllegalStateException(INHERITANCE_ERROR)
  }

  private val INHERITANCE_ERROR = "This trait should only be mixed into Fragment or Activity"

}

/**
 * A message to be sent to Server object so that they can initialize
 * themselves with whatever Android Service they need for their work.
 */
case class Initialize[APP <: EventfulApp](app: APP)

/**
 * This mixin turns the subclass of Application into an EventHub host.
 */
trait EventfulApp extends android.app.Application with ActorConversion {

  private val eventHubHandle = new Handler(new EventHub)

  def eventHub: Handler = eventHubHandle

}
