package edu.vanderbilt.vandyvans;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import edu.vanderbilt.vandyvans.models.FloatPair;
import edu.vanderbilt.vandyvans.models.Route;
import edu.vanderbilt.vandyvans.models.Routes;
import edu.vanderbilt.vandyvans.models.Stop;
import edu.vanderbilt.vandyvans.models.Van;
import edu.vanderbilt.vandyvans.services.Clients;
import edu.vanderbilt.vandyvans.services.Global;
import edu.vanderbilt.vandyvans.services.SyncromaticsClient;
import edu.vanderbilt.vandyvans.services.VandyVansClient;

import static edu.vanderbilt.vandyvans.services.Global.APP_LOG_ID;

/**
 * Controls the behaviour of the map. Usually the activity would act as
 * a controller, but we don't want to clutter the StopActivity with
 * too much responsibility.
 *
 * This Controller is a master of the SupportMapFragment that fills the
 * second slot of the ViewPager, the LinearLayout that is the bottom bar,
 * and the three Buttons for selecting Routes. It captures the click
 * events from the Buttons, fetches data from the services, and manipulates
 * the map in response to the data.
 *
 * Created by athran on 3/19/14.
 */
public class MapController implements Handler.Callback,
                                      View.OnClickListener {

    public static final float  DEFAULT_ZOOM  = 14.5f;
    public static final float  DEFAULT_WIDTH = 5;
    public static final String LOG_ID        = "MapController";

    private final Handler      bridge = new Handler(this);
    private final Clients      mClients;
    private final Global       mGlobal;

    private final SupportMapFragment mMapFragment;
    private final LinearLayout       mOverlayBar;
    private final Button             mBlueButton;
    private final Button             mRedButton;
    private final Button             mGreenButton;

    private Route        mCurrentRoute;
    private CameraUpdate mDefaultCamera = null;

    /**
     * The sole constructor.
     */
    public MapController(SupportMapFragment mapFrag,
                         LinearLayout       overlayBar,
                         Button             blueBtn,
                         Button             redBtn,
                         Button             greenBtn,
                         Clients            clients,
                         Global             global) {
        if (mapFrag == null) { throw new IllegalStateException("MapFragment is null"); }
        mMapFragment = mapFrag;

        mOverlayBar  = overlayBar;
        mBlueButton  = blueBtn;
        mRedButton   = redBtn;
        mGreenButton = greenBtn;

        mBlueButton  .setOnClickListener(this);
        mRedButton   .setOnClickListener(this);
        mGreenButton .setOnClickListener(this);

        mClients      = clients;
        mGlobal       = global;
        mCurrentRoute = Routes.BLUE;
    }

    /**
     * Handles the messages from the Services. Whenever the UI need any data, it will
     * send a request to the Services. The Services will process the request, fetching
     * the data from whatever source it has access to, then reply with the data.
     *
     * See: `routeSelected(route)`
     */
    @Override
    public boolean handleMessage(Message message) {
        if (message.obj instanceof VandyVansClient.WaypointResults)
            handleWaypointResult((VandyVansClient.WaypointResults) message.obj);

        if (message.obj instanceof VandyVansClient.StopResults)
            handleStopResults((VandyVansClient.StopResults) message.obj);

        if (message.obj instanceof SyncromaticsClient.VanResults)
            handleVanResults((SyncromaticsClient.VanResults) message.obj);

        return true;
    }

    /**
     * This method is called when one of the overlay buttons is clicked.
     * Send messages to the Services, requesting data on Stops, route path,
     * and van locations. Then clear and center the map.
     *
     * See: `onClick(view)`
     */
    public void routeSelected(Route route) {
        if (mClients == null) {
            throw new IllegalStateException("VandyClient is null");
        }

        mCurrentRoute = route;
        mOverlayBar.setBackgroundColor(mGlobal.getColorFor(mCurrentRoute));

        // Requesting data from the services.
        Message.obtain(mClients.vandyVans(), 0,
                       new VandyVansClient.FetchWaypoints(bridge, route))
                .sendToTarget();

        Message.obtain(mClients.vandyVans(), 0,
                       new VandyVansClient.FetchStops(bridge, route))
                .sendToTarget();

        Message.obtain(mClients.syncromatics(), 0,
                       new SyncromaticsClient.FetchVans(bridge, route))
                .sendToTarget();

        GoogleMap map = mMapFragment.getMap();
        if (map == null) { return; }

        map.clear();
        map.animateCamera(getDefaultCameraUpdate());
        map.setMyLocationEnabled(true);
    }

    @Override
    public void onClick(View view) {
        if (view == mBlueButton && mCurrentRoute != Routes.BLUE) {
            routeSelected(Routes.BLUE);

        } else if (view == mRedButton && mCurrentRoute != Routes.RED) {
            routeSelected(Routes.RED);

        } else if (view == mGreenButton && mCurrentRoute != Routes.GREEN) {
            routeSelected(Routes.GREEN);
        }
    }

    public void showOverlay() {
        mOverlayBar.setVisibility(View.VISIBLE);
    }

    public void hideOverlay() {
        mOverlayBar.setVisibility(View.INVISIBLE);
    }

    public void mapIsShown() {
        routeSelected(mCurrentRoute);
    }

    void handleWaypointResult(VandyVansClient.WaypointResults results) {
        drawWaypoints(results);
    }

    void handleStopResults(VandyVansClient.StopResults result) {
        drawStops(result);
    }

    void handleVanResults(SyncromaticsClient.VanResults result) {
        drawVans(result);
    }

    private boolean drawWaypoints(VandyVansClient.WaypointResults result) {
        GoogleMap map = mMapFragment.getMap();
        if (map == null) { return true; }

        PolylineOptions polyline = new PolylineOptions();
        polyline.color(mGlobal.getColorFor(mCurrentRoute));
        polyline.width(DEFAULT_WIDTH);

        for (FloatPair point : result.waypoints) {
            polyline.add(new LatLng(point.lat,
                                    point.lon));
        }

        map.addPolyline(polyline);
        return true;
    }

    private boolean drawStops(VandyVansClient.StopResults result) {
        GoogleMap map = mMapFragment.getMap();
        if (map == null) { return true; }

        for (Stop stop : result.stops) {
            map.addMarker(new MarkerOptions()
                                  .position(new LatLng(stop.latitude,
                                                       stop.longitude))
                                  .title(stop.name)
                                  .draggable(false));
        }

        return true;
    }

    private boolean drawVans(SyncromaticsClient.VanResults result) {
        GoogleMap map = mMapFragment.getMap();
        if (map == null) { return true; }

        Log.i(APP_LOG_ID, LOG_ID + " | Received this many Van results: " + result.vans.size());
        for (Van v : result.vans) {
            map.addMarker(
                    new MarkerOptions()
                            .position(new LatLng(v.location.lat,
                                                 v.location.lon))
                            .title(Integer.toString(v.percentFull) + "%")
                            .draggable(false)
                            .flat(true)
                            .icon(BitmapDescriptorFactory
                                          .fromResource(R.drawable.van_icon))
                            .anchor(0.5f, 0.5f));
        }

        return true;
    }

    private CameraUpdate getDefaultCameraUpdate() {
        if (mDefaultCamera == null) {
            mDefaultCamera =
                    CameraUpdateFactory.newLatLngZoom(
                            new LatLng(Global.DEFAULT_LATITUDE,
                                       Global.DEFAULT_LONGITUDE),
                            DEFAULT_ZOOM);
        }
        return mDefaultCamera;
    }

    static final class RedrawWithUpdatedVanLocation {

    }

}
