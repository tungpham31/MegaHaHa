package com.awwstream.android;

import android.app.AlertDialog;
import android.app.Application;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.PushService;

/**
 * The main {@link Application}.
 */
public final class MainApplication extends Application {
    private static final boolean IS_PRODUCTION = false;

    @Override
    public void onCreate() {
        super.onCreate();

        if (IS_PRODUCTION) {
            Parse.initialize(this, "nO6zS3yH8Z0N2PmVBeMzjqyqeQBt7vUzckNcxPHH",
                    "wjiAT5l9fqKuMLmSygPWvBjnViNGIpmzZb4PexXn");
        } else {
            Parse.initialize(this, "DZQNfL3wXVfWcKQky72xejuQfBjkeZK7g1DjkMrE",
                    "AYkmrdZSvwpElIYKvNkTjnc4tzuqoKfX29NsMFEt");
        }

        // Set up push notification.
        PushService.subscribe(this, "Hot", HotActivity.class);
        PushService.subscribe(this, "New", NewActivity.class);
        try {
            final int versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            PushService.subscribe(this, "Update-" + versionCode, UpdateActivity.class);
        } catch (NameNotFoundException e) {
            // Do nothing.
        }
        PushService.setDefaultPushCallback(this, HotActivity.class);
        ParseInstallation.getCurrentInstallation().saveInBackground();
    }
}
