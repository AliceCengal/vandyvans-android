package com.marsupial.eventhub

import android.app.{FragmentTransaction, Activity, Fragment}
import android.view.{View, ViewGroup, LayoutInflater}
import android.os.Bundle

/**
 * Some things to save a keystroke or two, and maybe get some
 * hacker street cred.
 *
 * Created by athran on 5/10/14.
 */
object Helpers {

  /**
   * For when using a third party library, and the library writer unfortunately made
   * the setter methods of their objects to return `void` instead of `this`,
   * so you can't do fluent method chaining.
   *
   * someLibraryObject <<< (
   *         _.methodOne(),
   *         _.setSomeProperty("What"),
   *         _.setAnotherProperty(42))
   */
  trait EasyChainCall {
    implicit def anyrefToChainCall[U](a: U): ChainCall[U] = new ChainCall[U](a)
  }

  class ChainCall[T](obj: T) {
    def <<<(calls: (T => Unit)*): T = {
      for (c <- calls) { c(obj) }
      obj
    }
  }

  /**
   * This mixin provides automatic view inflation. Just define the
   * desired layoutId. Also provides the component method for easy
   * access to view components.
   *
   * Created by athran on 4/19/14.
   */
  trait EasyFragment {
    self: Fragment =>

    def layoutId: Int

    override def onCreateView(inflater:  LayoutInflater,
                              container: ViewGroup,
                              saved:     Bundle): View = {
      inflater.inflate(layoutId, container, false)
    }

    def component[T](id: Int) = getView.findViewById(id).asInstanceOf[T]

  }

  /**
   * This mixin provide boilerplate free Fragment transactions.
   */
  trait EasyActivity {
    self: Activity =>

    def component[T](id: Int) = findViewById(id).asInstanceOf[T]

    def transaction(action: FragmentTransaction => Unit) {
      val ft = getFragmentManager.beginTransaction()
      action(ft)
      ft.commit()
    }

    def inflate(layout: Int) = getLayoutInflater.inflate(layout, null)

  }

}
