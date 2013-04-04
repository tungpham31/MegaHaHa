package com.awwstream.android;

import android.app.Application;
import android.content.SharedPreferences;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SaveCallback;

/**
 * The main {@link Application}.
 */
public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Parse.initialize(this, "nO6zS3yH8Z0N2PmVBeMzjqyqeQBt7vUzckNcxPHH",
                "wjiAT5l9fqKuMLmSygPWvBjnViNGIpmzZb4PexXn");

        final SharedPreferences pref = getSharedPreferences(getString(R.string.PREFS_NAME), 0);
        if (pref.getString("username", null) == null) {
            ParseUser.enableAutomaticUser();
            ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    pref.edit().putString("username", ParseUser.getCurrentUser().getUsername())
                            .commit();
                }
            });
        }
    }
}
