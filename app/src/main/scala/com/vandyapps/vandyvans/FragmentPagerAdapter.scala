package com.vandyapps.vandyvans

import android.app.{Fragment, FragmentTransaction, FragmentManager}
import android.os.Parcelable
import android.support.v4.view.PagerAdapter
import android.util.Log
import android.view.View
import android.view.ViewGroup

/* This may or may not be very illegal... */
abstract class FragmentPagerAdapter(var mFragmentManager: FragmentManager) extends PagerAdapter {
  import FragmentPagerAdapter._

  private var mCurTransaction: FragmentTransaction = null
  private var mCurrentPrimaryItem: Fragment = null

  /**
   * Return the Fragment associated with a specified position.
   */
  def getItem(position: Int): Fragment

  override def startUpdate(container: ViewGroup) {}

  override def instantiateItem(container: ViewGroup, position: Int): AnyRef = {
    if (mCurTransaction == null) {
      mCurTransaction = mFragmentManager.beginTransaction()
    }

    val itemId = getItemId(position)

    // Do we already have this fragment?
    val name = makeFragmentName(container.getId, itemId)
    val fragment =
      Option(mFragmentManager.findFragmentByTag(name)) match {
        case Some(f) =>
          if (DEBUG) Log.v(TAG, "Attaching item #" + itemId + ": f=" + f)
          mCurTransaction.attach(f)
          f
        case None =>
          val f = getItem(position)
          if (DEBUG) Log.v(TAG, "Adding item #" + itemId + ": f=" + f)
          mCurTransaction.add(
            container.getId, f,
            makeFragmentName(container.getId, itemId))
          f
      }

    if (fragment ne mCurrentPrimaryItem) {
      fragment.setMenuVisibility(false)
      fragment.setUserVisibleHint(false)
    }

    fragment
  }

  override def destroyItem(container: ViewGroup, position: Int, obj: AnyRef) {
    obj match {
      case frag: Fragment =>
        if (mCurTransaction == null) {
          mCurTransaction = mFragmentManager.beginTransaction()
        }
        if (DEBUG) Log.v(TAG,
          s"Detaching item #${getItemId(position)}: f=$obj v=${frag.getView}")
        mCurTransaction.detach(frag)

      case _ => /* should be error */
    }
  }

  override def setPrimaryItem(container: ViewGroup, position: Int, obj: AnyRef) {
    obj match {
      case frag: Fragment =>
        if (frag ne mCurrentPrimaryItem) {
          Option(mCurrentPrimaryItem).foreach { item =>
            item.setMenuVisibility(false)
            item.setUserVisibleHint(false)
          }
          frag.setMenuVisibility(true)
          frag.setUserVisibleHint(true)
          mCurrentPrimaryItem = frag
        }
    }
  }

  override def finishUpdate(container: ViewGroup) {
    if (mCurTransaction != null) {
      mCurTransaction.commitAllowingStateLoss()
      mCurTransaction = null
      mFragmentManager.executePendingTransactions()
    }
  }

  override def isViewFromObject(view: View, obj: AnyRef): Boolean = {
    obj.asInstanceOf[Fragment].getView eq view
  }

  override def saveState(): Parcelable = {
    null
  }

  override def restoreState(state: Parcelable, loader: ClassLoader) {}

  /**
   * Return a unique identifier for the item at the given position.
   *
   * <p>The default implementation returns the given position.
   * Subclasses should override this method if the positions of items can change.</p>
   *
   * @param position Position within this adapter
   * @return Unique identifier for the item at position
   */
  def getItemId(position: Int): Long = position


}

object FragmentPagerAdapter {
  private def TAG = "FragmentPagerAdapter"
  private def DEBUG = false

  private def makeFragmentName(viewId: Int, id: Long): String = {
    s"android:switcher:$viewId:$id"
  }
}