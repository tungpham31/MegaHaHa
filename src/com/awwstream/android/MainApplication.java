package com.awwstream.android;

import android.app.Application;

import com.parse.Parse;

/**
 * The main {@link Application}.
 */
public final class MainApplication extends Application {
	public static final String MODE = "DEBUG";
	
	@Override
	public void onCreate() {
		super.onCreate();

		if (MODE.equals("RELEASE")) {
			Parse.initialize(this, "nO6zS3yH8Z0N2PmVBeMzjqyqeQBt7vUzckNcxPHH",
					"wjiAT5l9fqKuMLmSygPWvBjnViNGIpmzZb4PexXn");
		} else
			Parse.initialize(this, "DZQNfL3wXVfWcKQky72xejuQfBjkeZK7g1DjkMrE",
					"AYkmrdZSvwpElIYKvNkTjnc4tzuqoKfX29NsMFEt");
	}
}
