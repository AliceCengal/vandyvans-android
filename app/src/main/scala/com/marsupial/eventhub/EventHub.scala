package com.marsupial.eventhub

import scala.collection.mutable
import scala.ref.WeakReference

import android.os.Handler

/**
 * A global event hub that allows different components of
 * the app to communicate without explicit coupling.
 */
object EventHub {

  /**
   * Subscribe to the global event stream
   */
  case object Subscribe

  /**
   * Unsubscribe from the global event stream
   */
  case object Unsubscribe

}

private[eventhub] class EventHub extends Server {
  import EventHub._

  private val subscribers = mutable.Set.empty[WeakReference[Handler]]

  override def handleRequest(incoming: AnyRef) {
    incoming match {
      case Subscribe   => subscribers.add(new WeakReference[Handler](requester))
      case Unsubscribe => purgeSubscribers(requester)
      case a: AnyRef   => broadcastEvent(a)
    }
  }

  private def purgeSubscribers(who: Handler): Unit = {
    subscribers.retain((weakHandler) => {
      weakHandler.get.isDefined && (weakHandler() ne who)
    })
  }

  private def broadcastEvent(event: AnyRef): Unit = {
    for (
      weakRefToHandler <- subscribers;
      maybeHandler     <- weakRefToHandler.get
    ) {
      maybeHandler ! event
    }
  }

}
