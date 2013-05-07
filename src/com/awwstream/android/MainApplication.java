package com.awwstream.android;

import android.app.Application;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import com.parse.Parse;
import com.parse.ParseInstallation;
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
        PushService.setDefaultPushCallback(this, HotActivity.class);
        PushService.subscribe(this, "Hot", HotActivity.class);
        PushService.subscribe(this, "New", NewActivity.class);
        try {
            final int versionCode =
                    getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            PushService.subscribe(this, "Update-" + versionCode, UpdateActivity.class);
            
            // Update Parse Installation.
            ParseInstallation parseInstallation = ParseInstallation.getCurrentInstallation();
            parseInstallation.put("versionCode", Integer.valueOf(versionCode));
            parseInstallation.saveInBackground();
        } catch (NameNotFoundException e) {
            // Do nothing.
        }
    }
}
