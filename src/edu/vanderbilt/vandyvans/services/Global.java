package edu.vanderbilt.vandyvans.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.Map;

import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provider;
import roboguice.RoboGuice;
import com.parse.Parse;

import edu.vanderbilt.vandyvans.R;
import edu.vanderbilt.vandyvans.ReminderController;
import edu.vanderbilt.vandyvans.models.Routes;
import edu.vanderbilt.vandyvans.models.Route;

/**
 * Holds static references to the vital backend services which are accessible
 * globally in the application. This Singleton-bundle is initialized by the
 * `onCreate` call made by the Android system as soon as the process for this
 * app is created.
 * 
 * @author athran
 *
 */
public final class Global extends android.app.Application {

    //https://maps.google.com/?ll=36.143905,-86.805811&spn=0.012545,0.024097&t=h&z=16
    public static final double DEFAULT_LONGITUDE = -86.805811;
    public static final double DEFAULT_LATITUDE  = 36.143905;
    public static final String APP_LOG_ID        = "VandyVans";
    public static final String APP_PREFERENCES   = "VandyVansPreferences";

    private ClientsSingleton         mClientSingleton;
    private SimpleReminderController mReminderController;

    private int COLOR_RED;
    private int COLOR_BLUE;
    private int COLOR_GREEN;
    private int COLOR_BLACK;

    @Override
    public void onCreate() {
        super.onCreate();
        initializeGlobalState();

        Resources res = getResources();
        COLOR_BLUE  = res.getColor(R.color.blue_argb);
        COLOR_RED   = res.getColor(R.color.red_argb);
        COLOR_GREEN = res.getColor(R.color.green_argb);
        COLOR_BLACK = res.getColor(android.R.color.black);

        Parse.initialize(this,
                         "6XOkxBODp8HZANJaxFhEfSFPZ8H93Pt9531Htt1X",
                         "61wOewMMN0YISmX3UM79PGssnTsz1NfkOOMOsHMm");
    }

    public int getRed() { return COLOR_RED; }

    public int getBlue() { return COLOR_BLUE; }

    public int getGreen() { return COLOR_GREEN; }

    public int getColorFor(Route route) {
        if (route == Routes.BLUE)  return COLOR_BLUE;
        if (route == Routes.RED)   return COLOR_RED;
        if (route == Routes.GREEN) return COLOR_GREEN;
        return COLOR_BLACK;
    }

    public Global setShowSelfLocation(boolean showLocation) {
        return this;
    }

    public boolean isShowingLocation() {
        return true;
    }

    /**
     * Called in the `onCreate` of android.app.Application when the process
     * for this app is first created. This will initialize the service
     * infrastructure that provide data to the UI.
     */
    private void initializeGlobalState() {

        // Intialize the background thread to be used by the services.
        final HandlerThread thread = new HandlerThread("BackgroundThread");
        thread.start();

        // Create an Object to hold on to the services.
        mClientSingleton = new ClientsSingleton(thread, this);
        mReminderController = new SimpleReminderController(this);

        mReminderController.start();

        // Create providers to inject the Service Holder to anybody who
        // needs it.
        RoboGuice.setBaseApplicationInjector(
                this,
                RoboGuice.DEFAULT_STAGE,
                RoboGuice.newDefaultRoboModule(this),
                new Module() {
                    @Override
                    public void configure(Binder binder) {
                        binder.bind(Clients.class)

                                // inject the fucking injector!
                                .toProvider(new Provider<Clients>() {
                                    @Override
                                    public Clients get() {
                                        return mClientSingleton;
                                    }
                                });

                        binder.bind(Global.class)
                                .toProvider(new Provider<Global>() {
                                    @Override
                                    public Global get() {
                                        return Global.this;
                                    }
                                });

                        binder.bind(ReminderController.class)
                                .toProvider(new Provider<ReminderController>() {
                                    @Override
                                    public ReminderController get() {
                                        return mReminderController;
                                    }
                                });
                    }
                });

    }

    private static final class ClientsSingleton implements Clients {

        final Handler vandyVansClient;
        final Handler syncromaticsClient;

        ClientsSingleton(HandlerThread serviceThread, Context ctx) {
            vandyVansClient = new Handler(serviceThread.getLooper(),
                                          new VandyVansClient());
            Message.obtain(vandyVansClient, 0,
                           new Initialize(ctx)).sendToTarget();

            syncromaticsClient = new Handler(serviceThread.getLooper(),
                                             new SyncromaticsClient());
            Message.obtain(syncromaticsClient, 0,
                           new Initialize(ctx)).sendToTarget();
        }

        @Override
        public Handler vandyVans() {
            return vandyVansClient;
        }

        @Override
        public Handler syncromatics() {
            return syncromaticsClient;
        }
    }



    public static final class Failure {
        public final Object    originalMessage;
        public final Exception error;
        public final String    extraInfo;
        public Failure(Object _msg,
                       Exception _error,
                       String _info) {
            originalMessage = _msg;
            error           = _error;
            extraInfo       = _info;
        }
    }

    static final class Initialize {
        final Context ctx;
        public Initialize(Context _ctx) {
            ctx = _ctx;
        }
    }

    static InputStream get(String url) throws IOException {
        return new URL(url).openStream();
    }

    static InputStream post(String url, String params) throws IOException {
        URLConnection conn = new URL(url).openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);

        Writer writer = new OutputStreamWriter(conn.getOutputStream());
        writer.write(params);
        writer.flush();

        return conn.getInputStream();
    }

    static InputStream postUrlRequest(String url, Map<String,String> params) throws IOException {
        StringBuilder builder = new StringBuilder(url);
        builder.append("?");

        for (String key : params.keySet()) {
            builder
                    .append(key)
                    .append("=")
                    .append(URLEncoder.encode(params.get(key),
                                              "UTF-8"))
                    .append("&");
        }

        builder.deleteCharAt(builder.length()-1);

        Log.i("VandyVansClient", builder.toString());
        URLConnection conn = new URL(builder.toString()).openConnection();
        conn.setDoInput(true);
        conn.setUseCaches(false);

        return conn.getInputStream();
    }

    static String encryptPassword(String password)
    {
        String sha1 = "";
        try
        {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(password.getBytes("UTF-8"));
            sha1 = byteToHex(crypt.digest());
        }
        catch(NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        catch(UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
        return sha1;
    }

    static String byteToHex(final byte[] hash)
    {
        Formatter formatter = new Formatter();
        for (byte b : hash)
        {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

}
