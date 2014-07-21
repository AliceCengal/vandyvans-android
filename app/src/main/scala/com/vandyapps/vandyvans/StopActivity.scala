package com.vandyapps.vandyvans

import java.util.Locale

import android.app._
import android.os.Bundle
import android.support.v4.view.ViewPager
import android.view.{KeyEvent, MenuItem, Menu}

import com.marsupial.eventhub.AppInjection
import com.marsupial.eventhub.Helpers.EasyActivity
import com.vandyapps.vandyvans.services.Global

class StopActivity extends Activity
    with ActionBar.TabListener with EasyActivity with AppInjection[Global]
{
  lazy val stopFragment = new StopsFragment
  lazy val mapFrag      = new StopsMap
  lazy val pagerAdapter = new SectionsPagerAdapter(this.getFragmentManager)

  def viewPager = component[ViewPager](R.id.pager)

  override def onCreate(saved: Bundle) {
    super.onCreate(saved)
    setContentView(R.layout.activity_stop)
    Option(getActionBar).foreach { _.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS) }

    viewPager.setAdapter(pagerAdapter)
    viewPager.setOnPageChangeListener(PageChangeListener)

    for (action <- Option(getActionBar);
         i <- 0 until pagerAdapter.getCount) {
      action.addTab(action.newTab()
          .setText(pagerAdapter.getPageTitle(i))
          .setTabListener(this))
    }
  }

  override def onCreateOptionsMenu(menu: Menu) = {
    getMenuInflater.inflate(R.menu.stop, menu); true
  }

  override def onOptionsItemSelected(item: MenuItem) = {
    if (item.getItemId == R.id.action_settings) {
      AboutsActivity.open(this); true
    } else super.onOptionsItemSelected(item)
  }

  override def onKeyUp(keyCode: Int, event: KeyEvent) = {
    if (keyCode == KeyEvent.KEYCODE_BACK &&
        getActionBar.getSelectedNavigationIndex == 1) {
      getActionBar.setSelectedNavigationItem(0)
      true
    } else super.onKeyUp(keyCode, event)
  }

  override def onTabSelected(tab: ActionBar.Tab, ft: FragmentTransaction) {
    viewPager.setCurrentItem(tab.getPosition)
  }

  override def onTabUnselected(a: ActionBar.Tab, b: FragmentTransaction) {}

  override def onTabReselected(a: ActionBar.Tab, b: FragmentTransaction) {}

  object PageChangeListener extends ViewPager.SimpleOnPageChangeListener {
    override def onPageSelected(position: Int) {
      Option(getActionBar).foreach { _.setSelectedNavigationItem(position) }
    }
  }

  class SectionsPagerAdapter(fm: FragmentManager) extends FragmentPagerAdapter(fm) {

    override def getItem(position: Int): Fragment = {
      position match {
        case 0 => stopFragment
        case 1 => mapFrag
        case _ => throw new IllegalStateException("There's only two tabs")
      }
    }

    override def getCount = 2

    override def getPageTitle(position: Int): CharSequence = {
      val l = Locale.getDefault
      position match {
        case 0 => getString(R.string.stops_label).toUpperCase(l)
        case 1 => getString(R.string.map_label).toUpperCase(l)
        case _ => ""
      }
    }
  }

}
