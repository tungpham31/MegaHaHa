package com.awwstream.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;
import com.google.analytics.tracking.android.EasyTracker;

/**
 * An UI-less {@link Activity} to share video.
 */
public final class ShareActivity extends Activity {
    private static final String YOU_TUBE_URL = "http://www.youtube.com/watch?v=";
    private static final String YOU_TUBE_SECURE_URL = "https://www.youtube.com/watch?v=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String link = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        final String subject = getIntent().getStringExtra(Intent.EXTRA_SUBJECT);
        if (TextUtils.isEmpty(link) || TextUtils.isEmpty(subject)
                || (!link.startsWith(YOU_TUBE_URL) && !link.startsWith(YOU_TUBE_SECURE_URL))) {
            Toast.makeText(this, R.string.share_error_message, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        final String videoId;
        if (link.startsWith(YOU_TUBE_URL)) {
            videoId = link.substring(YOU_TUBE_URL.length()).split("&")[0];
        } else {
            videoId = link.substring(YOU_TUBE_SECURE_URL.length()).split("&")[0];
        }
        final String title = subject.split("\"")[1];

        Utils.promoteVideo(videoId, title);

        Toast.makeText(this, R.string.share_message, Toast.LENGTH_SHORT).show();

        FlurryAgent.logEvent("Promote");
        EasyTracker.getInstance().setContext(this);
        EasyTracker.getTracker().sendEvent("UI", "Click", "Promote", null);

        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();

        FlurryAgent.onStartSession(this, "4QVGFH2RQW3ZM5X4W2C3");
        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        FlurryAgent.onEndSession(this);
        EasyTracker.getInstance().activityStop(this);
    }
}
