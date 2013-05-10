package com.awwstream.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

/**
 * An UI-less {@link Activity} to install new app.
 */
public final class InstallActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.i("Counter", "In Install");

        final Intent intent = new Intent(Intent.ACTION_VIEW);
        try {
            final ParseObject newApp = new ParseQuery("New_App").getFirst();
            intent.setData(Uri.parse(newApp.getString("url")));
            startActivity(intent);
        } catch (ParseException e) {
            // Do nothing.
        }

        finish();
    }
}
