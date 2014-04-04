package edu.vanderbilt.vandyvans;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import roboguice.activity.RoboActivity;

/**
 *
 * Created by athran on 4/1/14.
 */
public class NotificationSettingsActivity extends RoboActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    public static void open(Context ctx) {
        Intent i = new Intent(ctx, NotificationSettingsActivity.class);
        ctx.startActivity(i);
    }

}
