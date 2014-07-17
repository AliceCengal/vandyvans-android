package com.marsupial.eventhub

import android.os.{Message, Handler}
import android.app.{Activity, Fragment}

/**
 * A basic Actor that uses Android's messaging framework.
 */
trait HandlerActor {
  def !(msg: AnyRef): Unit
  def ?(msg: AnyRef)(implicit requester: Handler): Unit
  def tell(msg: AnyRef): Unit
  def request(msg: AnyRef)(implicit requester: Handler): Unit
}

/**
 * A wrapper around Android's Handler which adds convenient
 * Erlang-inspired Actor DSL. Using implicit conversion by mixing
 * in the ActorConversion trait, a normal Handler can be treated
 * as if it is an ActorShell.
 */
class ActorShell(h: Handler) extends HandlerActor {
  override def !(msg: AnyRef): Unit = {
    Message.obtain(h, 0, msg).sendToTarget()
  }

  override def tell(msg: AnyRef): Unit = {
    Message.obtain(h, 0, msg).sendToTarget()
  }

  override def ?(msg: AnyRef)(implicit requester: Handler): Unit = {
    Message.obtain(h, 0, (requester, msg)).sendToTarget()
  }

  override def request(msg: AnyRef)(implicit requester: Handler): Unit = {
    Message.obtain(h, 0, (requester, msg)).sendToTarget()
  }
}

/**
 * Mixin to provide the Erlang-inspired Actor DSL to Android's Handler.
 *
 * Mix this trait into a class, or extends the ChattyFragment/ChattyActivity
 * or Server traits for a more complete package.
 */
trait ActorConversion {
  implicit def handlerToActor(h: Handler): HandlerActor = new ActorShell(h)
}

/**
 * Mixin for Fragments that want to communicate over the event bus
 *
 * This trait automatically registers and unregisters the Fragment from
 * the global event bus according to the Fragment lifecycle. It also
 * provide an implicit HandlerActor for use with `HandlerActor::request`,
 * as well as the `ActorConversion` mixin
 *
 * The Fragment that wants to mix this trait in must implement Handler.Callback
 * to handle incoming messages, and FragmentInjection for access to the EventHub.
 *
 * Created by athran on 5/8/14.
 */
trait ChattyFragment extends Fragment
                             with ActorConversion
{
  self: Handler.Callback with AppInjection[_ <: EventfulApp] =>

  implicit lazy val communicator = new Handler(this)

  override def onStart() {
    super.onStart()
    app.eventHub request EventHub.Subscribe
  }

  override def onStop() {
    super.onStop()
    app.eventHub request EventHub.Unsubscribe
  }

}

/**
 * Same as ChattyFragment, but for Activity.
 */
trait ChattyActivity extends Activity
                             with ActorConversion
{
  self: Handler.Callback with AppInjection[_ <: EventfulApp] =>

  implicit lazy val communicator = new Handler(this)

  override def onStart() {
    super.onStart()
    app.eventHub request EventHub.Subscribe
  }

  override def onStop() {
    super.onStop()
    app.eventHub request EventHub.Unsubscribe
  }

}

/**
 * Template for backend service. Not the same thing as android.app.Service,
 * although the class that extends this trait could also extend Service because
 * the async mode of communication through Handlers is the same.
 */
trait Server extends Handler.Callback
                     with DefaultNullProvider
                     with ActorConversion
{
  /**
   * Override this to handle requests.
   */
  def handleRequest(req: AnyRef): Unit

  /**
   * The sender of the request.
   */
  var requester: Handler = null

  override def handleMessage(msg: Message): Boolean = {
    msg.obj match {
      case (r: Handler, req: AnyRef) =>
        requester = r
        handleRequest(req)
      case a: AnyRef =>
        requester = nullHandler
        handleRequest(a)
    }
    true
  }
}

/**
 * Similar to the DeadLetterOffice in Akka.
 */
trait NullProvider {

  def nullHandler: Handler

}

trait DefaultNullProvider extends NullProvider {

  override def nullHandler: Handler = SimpleNullHandler

}

private object SimpleNullHandler extends Handler {

  override def handleMessage(msg: Message) {
    /* Do Nothing */
  }

}
