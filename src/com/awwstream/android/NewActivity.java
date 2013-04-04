package com.awwstream.android;

import android.app.Activity;

/**
 * The {@link Activity} to play New videos.
 */
public final class NewActivity extends UserActivity {
    protected String sortKey() {
        return "created_at";
    }
}
