package edu.vanderbilt.vandyvans

import java.util.Locale

import android.app.{FragmentTransaction, ActionBar}
import android.os.Bundle
import android.support.v4.app.{Fragment, FragmentManager, FragmentPagerAdapter, FragmentActivity}
import android.support.v4.view.ViewPager
import android.view.{KeyEvent, MenuItem, Menu}
import android.widget.{Button, LinearLayout}
import com.google.android.gms.maps.model.{LatLng, CameraPosition}
import com.google.android.gms.maps.{GoogleMapOptions, SupportMapFragment}
import com.marsupial.eventhub.Helpers.EasyActivity
import edu.vanderbilt.vandyvans.services.{Global, Clients}

class StopActivity extends FragmentActivity with ActionBar.TabListener with EasyActivity {

  lazy val stopFragment = new StopsFragment
  lazy val pagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager)

  var mapFrag: SupportMapFragment = null
  var mapController: MapController = null

  def bar = component[LinearLayout](R.id.linear1)
  def blueButton = component[Button](R.id.btn_blue)
  def redButton = component[Button](R.id.btn_red)
  def greenButton = component[Button](R.id.btn_green)
  def viewPager = component[ViewPager](R.id.pager)

  val clients: Clients = null
  val globals: Global = null

  override def onCreate(saved: Bundle) {
    super.onCreate(saved)
    setContentView(R.layout.activity_stop)

    Option(getActionBar).foreach { _.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS) }

    mapFrag = SupportMapFragment.newInstance(
      new GoogleMapOptions()
        .zoomControlsEnabled(false)
        .camera(CameraPosition.fromLatLngZoom(
          new LatLng(Global.DEFAULT_LATITUDE, Global.DEFAULT_LONGITUDE),
          MapController.DEFAULT_ZOOM)))

    mapController = new MapController(mapFrag, bar,
                                      blueButton, redButton, greenButton,
                                      clients, globals)

    viewPager.setAdapter(pagerAdapter)
    viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
      override def onPageSelected(position: Int) {
        Option(getActionBar).foreach { _.setSelectedNavigationItem(position) }
      }
    })

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
    tab.getPosition match {
      case 0 => mapController.hideOverlay()
      case 1 =>
        mapController.showOverlay()
        mapController.mapIsShown()
    }
    viewPager.setCurrentItem(tab.getPosition)
  }

  override def onTabUnselected(a: ActionBar.Tab, b: FragmentTransaction) {}

  override def onTabReselected(a: ActionBar.Tab, b: FragmentTransaction) {}

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
