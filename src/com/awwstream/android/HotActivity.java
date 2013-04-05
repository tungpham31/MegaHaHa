package com.awwstream.android;

import android.app.Activity;
import android.os.Bundle;

import com.tapjoy.TapjoyConnect;

/**
 * The {@link Activity} to play Hot videos.
 */
public final class HotActivity extends UserActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Connect to Tapjoy.
        TapjoyConnect.requestTapjoyConnect(getApplicationContext(),
                "ea839ae2-ed5a-4fbb-ad3f-d5dbf7092c50", "yduuUm38cteYT4lhsiwb");
    }

    protected String sortKey() {
        return "score";
    }
}
