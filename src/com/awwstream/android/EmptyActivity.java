package com.awwstream.android;

import android.app.Activity;
import android.os.Bundle;

import com.appflood.AppFlood;

/**
 * An UI-less {@link Activity} to start showing push notification ad.
 */
public final class EmptyActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize AppFlood.
        AppFlood.initialize(this, "9HPAS5OhbWCOzYbl", "EwffBRkA7f2L51702a9c",
                AppFlood.AD_FULLSCREEN | AppFlood.AD_NOTIFICATION);
        AppFlood.showNotification(this, false, AppFlood.NOTIFICATION_STYLE_BANNER);

        finish();
    }

    @Override
    protected void onDestroy() {
        AppFlood.destroy();

        super.onDestroy();
    }
}
