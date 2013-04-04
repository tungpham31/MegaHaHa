package com.awwstream.android;

import android.app.Activity;

/**
 * The {@link Activity} to play New videos.
 */
public class NewActivity extends HotActivity {
    protected String sortKey() {
        return "created_at";
    }
}
