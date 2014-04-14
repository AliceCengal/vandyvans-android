package edu.vanderbilt.vandyvans.services;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.util.JsonWriter;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.parse.ParseObject;

import edu.vanderbilt.vandyvans.models.FloatPair;
import edu.vanderbilt.vandyvans.models.Report;
import edu.vanderbilt.vandyvans.models.Route;
import edu.vanderbilt.vandyvans.models.Stop;

import static edu.vanderbilt.vandyvans.services.Global.APP_LOG_ID;

/**
 * Implements the requests to the `vandyvans.com` server.
 *
 * Created by athran on 3/16/14.
 */
final class VandyVansClient implements Handler.Callback {

    private static final String     LOG_TAG    = "VandyVansClient";
    private static final String     BASE_URL   = "http://vandyvans.com";
    private static final String     REPORT_URL = "http://studentorgs.vanderbilt.edu/vandymobile/bugReport.php";
    private static final JsonParser PARSER     = new JsonParser();

    private static final String REPORT_CLASSNAME = "VVReport";
    private static final String REPORT_USEREMAIL = "userEmail";
    private static final String REPORT_BODY      = "body";
    private static final String REPORT_ISBUG     = "isBugReport";
    private static final String REPORT_NOTIFY    = "notifyWhenResolved";

    private static final String ROUTE_CACHE_DATE = "VandyVansClientRouteCacheDate";
    private static final String ROUTE_DATA       = "VandyVansClientRouteData";
    private static final String STOPS_CACHE_DATE = "VandyVansClientStopsCacheDate";
    private static final String STOPS_DATA       = "VandyVansClientStopsData";
    private static final long   CACHE_EXPIRATION = 14 * 24 * 3600 * 1000;

    private SharedPreferences mPrefs;

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.obj instanceof Global.Initialize)
            return init(((Global.Initialize) msg.obj).ctx);

        else if (msg.obj instanceof Global.FetchStops)
            return fetchStops(
                    ((Global.FetchStops) msg.obj).from,
                    ((Global.FetchStops) msg.obj).route);

        else if (msg.obj instanceof Global.FetchWaypoints)
            return waypoints(
                    ((Global.FetchWaypoints) msg.obj).from,
                    ((Global.FetchWaypoints) msg.obj).route);

        else if (msg.obj instanceof Report)
            return postReportUsingParseApi((Report) msg.obj);

        else return false;
    }

    private boolean init(Context ctx) {
        Log.d(APP_LOG_ID, LOG_TAG + " | Initialization");
        mPrefs = ctx.getSharedPreferences(Global.APP_PREFERENCES,
                                          Context.MODE_PRIVATE);
        return true;
    }

    private boolean fetchStops(Handler requester, Route r) {

        String cacheId     = STOPS_DATA + r.id;
        String dateCacheId = STOPS_CACHE_DATE + r.id;

        if (mPrefs.contains(cacheId)) {
            if (!isCacheExpired(dateCacheId)) {
                List<Stop> result = parseStopResult(
                        new StringReader(
                                mPrefs.getString(cacheId, "")));

                if (result.isEmpty()) {
                    invalidateCache(cacheId);
                    // jump to DO_HTTP_FETCH

                } else {
                    requester
                            .obtainMessage(0, new Global.StopResults(result))
                            .sendToTarget();
                    return true; // SHORTCUT
                }

            } else {
                invalidateCache(cacheId);
                // jump to DO_HTTP_FETCH
            }
        }

        // DO_HTTP_FETCH
        final StringBuilder buffer = new StringBuilder(BASE_URL)
                .append("/Route/")
                .append(r.id)
                .append("/Direction/0/Stops");

        try {
            final Reader reader    = new InputStreamReader(Global.get(buffer.toString()));
            final String rawResult = readAll(reader);
            reader.close();

            final List<Stop> result = parseStopResult(new StringReader(rawResult));

            requester
                    .obtainMessage(0, new Global.StopResults(result))
                    .sendToTarget();

            storeCacheRawDataUpdateDate(cacheId, rawResult, dateCacheId);

        } catch (Exception e) {
            Log.e(APP_LOG_ID, LOG_TAG + " | Failed to get Stops for Route.");
            Log.e(APP_LOG_ID, LOG_TAG + " | URL: " + buffer.toString());
            Log.e(APP_LOG_ID, e.getMessage());
        }
        return true;
    }

    private static List<Stop> parseStopResult(Reader reader) {
        List<Stop> result = new LinkedList<Stop>();

        for (JsonElement elem : PARSER.parse(reader).getAsJsonArray()) {
            JsonObject obj = elem.getAsJsonObject();
            result.add(new Stop(
                    obj.get(Stop.TAG_ID).getAsInt(),
                    obj.get(Stop.TAG_NAME).getAsString(),
                    obj.get(Stop.TAG_IMAGE).getAsString(),
                    obj.get(Stop.TAG_LAT).getAsDouble(),
                    obj.get(Stop.TAG_LON).getAsDouble(),
                    obj.get(Stop.TAG_RTPI).getAsInt()));
        }

        return result;
    }

    private boolean waypoints(Handler requester, Route r) {

        String cacheId     = ROUTE_DATA + r.id;
        String cacheDateId = ROUTE_CACHE_DATE + r.id;

        if (mPrefs.contains(cacheId)) {
            if (!isCacheExpired(cacheId)) {
                List<FloatPair> result = parseWaypointResult(
                        new StringReader(
                                mPrefs.getString(cacheId, "")));
                if (result.isEmpty()) {
                    invalidateCache(cacheId);
                    // jump to DO_HTTP_FETCH

                } else {
                    requester
                            .obtainMessage(0,
                                           new Global.WaypointResults(result))
                            .sendToTarget();
                    return true; // SHORTCUT
                }

            } else {
                invalidateCache(cacheId);
                // jump to DO_HTTP_FETCH
            }
        }

        // DO_HTTP_FETCH
        StringBuilder buffer = new StringBuilder(BASE_URL)
                .append("/Route/")
                .append(r.id)
                .append("/Waypoints");

        try {
            final Reader reader    = new InputStreamReader(Global.get(buffer.toString()));
            final String rawResult = readAll(reader);
            reader.close();

            List<FloatPair> result = parseWaypointResult(new StringReader(rawResult));

            requester
                    .obtainMessage(0,
                            new Global.WaypointResults(result))
                    .sendToTarget();

            storeCacheRawDataUpdateDate(cacheId, rawResult, cacheDateId);

        } catch (Exception e) {
            Log.e(APP_LOG_ID, LOG_TAG + " | Failed to get Waypoints for Route.");
            Log.e(APP_LOG_ID, LOG_TAG + " | URL: " + buffer.toString());
            Log.e(APP_LOG_ID, e.getMessage());
        }
        return true;
    }

    private static List<FloatPair> parseWaypointResult(Reader reader) {
        List<FloatPair> result = new LinkedList<FloatPair>();
        for (JsonElement elem : PARSER.parse(reader).getAsJsonArray()) {
            JsonObject obj = elem.getAsJsonObject();
            result.add(new FloatPair(
                    obj.get(FloatPair.TAG_LAT).getAsDouble(),
                    obj.get(FloatPair.TAG_LON).getAsDouble()));
        }

        return result;
    }

    private boolean postReport(Report report) {

        try {

            //final Map<String,String> params = generateKeyValuedOutput(report);
            final String jsonOutput = generateJsonOutput(report);

            final BufferedReader respReader = new BufferedReader(
                    new InputStreamReader(
                            //Global.postUrlRequest(REPORT_URL, params)
                            Global.post(REPORT_URL, jsonOutput)
                    ));

            Log.i(APP_LOG_ID, LOG_TAG + " | Vandy Vans server response for report.");
            //Log.i(LOG_TAG, buffer.toString());
            for (String line  = respReader.readLine(); // Yeah motherfucker
                        line != null;
                        line  = respReader.readLine()) {
                Log.i(APP_LOG_ID, line);
            }

        } catch (Exception e) {
            Log.e(APP_LOG_ID, LOG_TAG + " | Failed to send report");
            Log.e(APP_LOG_ID, report.toString());
            //Log.e(LOG_TAG, buffer.toString());
            Log.e(APP_LOG_ID, e.getMessage());
        }

        return true;
    }

    private String generateJsonOutput(Report report) {
        final StringWriter buffer = new StringWriter();
        final JsonWriter writer   = new JsonWriter(buffer);

        try {
            writer.beginObject();
            writer.name("verifyHash")
                    .value(Global.encryptPassword("vandyvansapp"));
            writer.name("isBugReport")
                    .value(report.isBugReport? "TRUE" : "FALSE");
            writer.name("senderAddress")
                    .value(report.senderAddress);
            writer.name("body")
                    .value(report.bodyOfReport);
            writer.name("notifyWhenResolved")
                    .value(report.notifyWhenResolved);
            writer.endObject();
        } catch (IOException e) {
            return "";
        }

        return buffer.toString();
    }

    private Map<String,String> generateKeyValuedOutput(Report report) {
        final Map<String,String> params = new HashMap<String,String>();
        params.put("verifyHash"        , Global.encryptPassword("vandyvansapp"));
        params.put("isBugReport"       , report.isBugReport? "TRUE" : "FALSE");
        params.put("senderAddress"     , report.senderAddress);
        params.put("body"              , report.bodyOfReport);
        params.put("notifyWhenResolved", report.notifyWhenResolved? "TRUE" : "FALSE");
        return params;
    }

    private boolean postReportUsingParseApi(Report report) {
        ParseObject reportObj = new ParseObject(REPORT_CLASSNAME);
        reportObj.put(REPORT_USEREMAIL, report.senderAddress);
        reportObj.put(REPORT_BODY     , report.bodyOfReport);
        reportObj.put(REPORT_ISBUG    , report.isBugReport);
        reportObj.put(REPORT_NOTIFY   , report.notifyWhenResolved);
        reportObj.saveEventually();
        return true;
    }

    /**
     * Cache is expired if it was made more than two weeks ago.
     */
    private boolean isCacheExpired(String cacheId) {
        final long currentTime = System.currentTimeMillis();
        final long cacheDate   = mPrefs.getLong(cacheId, currentTime);
        return (currentTime - cacheDate) > CACHE_EXPIRATION;
    }

    private void invalidateCache(String cacheId) {
        mPrefs.edit()
                .remove(cacheId)
                .apply();
    }

    private void storeCacheRawDataUpdateDate(String cacheId, String data, String cacheDateId) {
        mPrefs.edit()
                .putString(cacheId, data)
                .putLong(cacheDateId, System.currentTimeMillis())
                .apply();
    }

    private static String readAll(Reader reader) {
        StringWriter buffer = new StringWriter();

        try {
            for (int c = reader.read();
                     c > -1;
                     c = reader.read()) {
                buffer.write(c);
            }
        } catch (IOException e) {
            // Let it go!
        }

        return buffer.toString();
    }

}
