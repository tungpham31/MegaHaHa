package com.awwstream.android;

import android.app.Application;

import com.parse.Parse;

/**
 * The main {@link Application}.
 */
public final class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Parse.initialize(this, "nO6zS3yH8Z0N2PmVBeMzjqyqeQBt7vUzckNcxPHH",
                "wjiAT5l9fqKuMLmSygPWvBjnViNGIpmzZb4PexXn");
    }
}
