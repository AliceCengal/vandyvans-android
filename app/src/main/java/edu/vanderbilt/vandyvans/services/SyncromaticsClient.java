package edu.vanderbilt.vandyvans.services;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.vanderbilt.vandyvans.models.ArrivalTime;
import edu.vanderbilt.vandyvans.models.FloatPair;
import edu.vanderbilt.vandyvans.models.Route;
import edu.vanderbilt.vandyvans.models.Routes;
import edu.vanderbilt.vandyvans.models.Stop;
import edu.vanderbilt.vandyvans.models.Van;

import static edu.vanderbilt.vandyvans.services.Global.APP_LOG_ID;

/**
 * Implements the requests to the `api.syncromatics.com` server.
 *
 * Created by athran on 3/16/14.
 */
public final class SyncromaticsClient implements Handler.Callback {

    /**
     * To:    Syncromatics Client
     * Reply: `VanResults`
     */
    public static final class FetchVans {
        public final Route   route;
        public final Handler from;
        public FetchVans(Handler _from, Route _r) {
            route = _r;
            from  = _from;
        }
    }

    /**
     * To:    Syncromatics Client
     * Reply: `ArrivalTimeResults`
     */
    public static final class FetchArrivalTimes {
        public final Stop    stop;
        public final Handler from;
        public FetchArrivalTimes(Handler _from, Stop _stop) {
            stop = _stop;
            from = _from;
        }
    }

    public static final class VanResults {
        public final List<Van> vans;
        public VanResults(List<Van> _vans) {
            vans = _vans;
        }
    }

    public static final class ArrivalTimeResults {
        public final List<ArrivalTime> times;
        public ArrivalTimeResults(List<ArrivalTime> _times) {
            times = _times;
        }
    }

    private static final String LOG_TAG  = "SyncromaticsClient";
    private static final String BASE_URL = "http://api.syncromatics.com";
    private static final String API_KEY  = "?api_key=a922a34dfb5e63ba549adbb259518909";

    private static final JsonParser PARSER = new JsonParser();

    /*package*/ SyncromaticsClient() {}

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.obj instanceof Global.Initialize)
            return init();

        else if (msg.obj instanceof FetchVans)
            return vans(
                    ((FetchVans) msg.obj).from,
                    ((FetchVans) msg.obj).route);

        else if (msg.obj instanceof FetchArrivalTimes)
            return arrivalTimes(
                    ((FetchArrivalTimes) msg.obj).from,
                    ((FetchArrivalTimes) msg.obj).stop);

        else return false;
    }

    private boolean init() {
        Log.d(APP_LOG_ID, LOG_TAG + " | Initialization");
        return true;
    }

    private boolean vans(Handler requester, Route route) {
        final StringBuilder buffer = new StringBuilder(BASE_URL)
                .append("/Route/")
                .append(route.id)
                .append("/Vehicles")
                .append(API_KEY);

        try {
            Reader reader = new InputStreamReader(Global.get(buffer.toString()));
            List<Van> result = new LinkedList<Van>();
            for (JsonElement elem : PARSER.parse(reader).getAsJsonArray()) {
                JsonObject obj = elem.getAsJsonObject();
                result.add(new Van(
                        obj.get(Van.TAG_ID).getAsInt(),
                        obj.get(Van.TAG_PERCENT_FULL).getAsInt(),
                        new FloatPair(
                                obj.get(Van.TAG_LATS).getAsDouble(),
                                obj.get(Van.TAG_LOND).getAsDouble())));
            }

            /* JAVA 8

            List<Van> result =
                    PARSER.parse(reader).getAsJsonArray().stream()
                            .map((elem) -> elem.getAsJsonObject())
                            .map((obj) -> new Van(
                                    obj.get(Van.TAG_ID).getAsInt(),
                                    obj.get(Van.TAG_PERCENT_FULL).getAsInt(),
                                    new FloatPair(
                                            obj.get(Van.TAG_LATS).getAsDouble(),
                                            obj.get(Van.TAG_LOND).getAsDouble())))
                            .collect(Collectors.toList());
            */


            reader.close();

            Message.obtain(requester, 0,
                           new VanResults(result))
                    .sendToTarget();

        } catch (Exception e) {
            Log.e(APP_LOG_ID, LOG_TAG + " | Failed to get Vans for Route.");
            Log.e(APP_LOG_ID, LOG_TAG + " | URL: " + buffer.toString());
            Log.e(APP_LOG_ID, e.getMessage());
        }
        return true;
    }

    // http://api.syncromatics.com/Route/745/Stop/263473/Arrivals?api_key=a922a34dfb5e63ba549adbb259518909
    private boolean arrivalTimes(final Handler requester, final Stop stop) {
        //Log.d(LOG_TAG, "Arrival Time request received.");
        List<ArrivalTime> result = new LinkedList<ArrivalTime>();
        for (Route r : Routes.getAll()) {
            ArrivalTime time = readArrivalTimeForRoute(r, stop);
            if (time != null) {
                result.add(time);
            }
        }

        //Log.d(LOG_TAG, "This many Times fetched: " + result.size());

        Message.obtain(requester, 0,
                       new ArrivalTimeResults(result))
                .sendToTarget();
/*
        requester
                .obtainMessage(0,
                               new Global.ArrivalTimeResults(result))
                .sendToTarget();
*/
        return true;
    }

    private ArrivalTime readArrivalTimeForRoute(Route route, final Stop stop) {
        final StringBuilder buffer = new StringBuilder(BASE_URL)
                .append("/Route/")
                .append(route.id)
                .append("/Stop/")
                .append(stop.id)
                .append("/Arrivals")
                .append(API_KEY);

        ArrivalTime result = null;

        try {
            final Reader reader = new InputStreamReader(Global.get(buffer.toString()));
            final JsonObject responseObj = PARSER.parse(reader).getAsJsonObject();
            final JsonObject predictionObj = responseObj
                    .get("Predictions").getAsJsonArray()
                    .get(0).getAsJsonObject();

            result = new ArrivalTime(stop,
                                     route,
                                     predictionObj
                                             .get("Minutes")
                                             .getAsInt());

            reader.close();

        } catch (Exception e) {
            // This stop may not be in this route.
            // return null
            //Log.e(LOG_TAG, e.getMessage());
            //Log.e(LOG_TAG, buffer.toString());
        }

        return result;
    }
}
