package edu.vanderbilt.vandyvans;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import android.widget.Toast;
import roboguice.activity.RoboActivity;
import roboguice.inject.InjectView;

/**
 * Activity for allowing user to access the app's source code.
 *
 * Created by athran on 4/14/14.
 */
public class CodeAccessActivity extends RoboActivity {

    public static void open(Context ctx) {
        ctx.startActivity(new Intent(ctx, CodeAccessActivity.class));
    }

    private String LINK_TO_REPOSITORY;

    @InjectView(R.id.btn1) Button mOpenBrowser;
    @InjectView(R.id.btn2) Button mEmailLink;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.source_code_access);

        LINK_TO_REPOSITORY = getString(R.string.sourcecode_link);

        mOpenBrowser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openGithubPageInBrowser();
            }
        });

        mEmailLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendLinkInEmail();
            }
        });
    }

    private void openGithubPageInBrowser() {
        startActivity(
                new Intent(Intent.ACTION_VIEW,
                           Uri.parse(LINK_TO_REPOSITORY)));
    }

    private void sendLinkInEmail() {
        try {
            startActivity(Intent.createChooser(
                    new Intent(Intent.ACTION_SEND)
                            .setType("message/rfc822")
                            .putExtra(Intent.EXTRA_SUBJECT, "Link to VandyVans source code")
                            .putExtra(Intent.EXTRA_TEXT, LINK_TO_REPOSITORY),
                    "Send mail..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(
                    this,
                    "There are no email clients installed.",
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }

}
