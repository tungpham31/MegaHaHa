package com.awwstream.android;

import android.app.Activity;

/**
 * The {@link Activity} to play Hot videos.
 */
public final class HotActivity extends UserActivity {
    protected String sortKey() {
        return "score";
    }
}
