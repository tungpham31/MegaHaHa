package com.awwstream.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;

/**
 * An UI-less {@link Activity} to share video.
 */
public class ShareActivity extends Activity {
    private static final String YOU_TUBE_URL = "https://www.youtube.com/watch?v=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String link = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        if (TextUtils.isEmpty(link) || !link.startsWith(YOU_TUBE_URL)) {
            Toast.makeText(this, R.string.share_error_message, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        final String videoId = link.substring(YOU_TUBE_URL.length()).split("&")[0];
        final String title = getIntent().getStringExtra(Intent.EXTRA_SUBJECT).split("\"")[1];

        final ParseQuery query = new ParseQuery("Video");
        query.whereEqualTo("videoId", videoId);
        query.findInBackground(new FindCallback() {
            @Override
            public void done(List<ParseObject> videos, ParseException e) {
                if (videos == null || videos.isEmpty()) {
                    final ParseObject video = new ParseObject("Video");
                    video.put("videoId", videoId);
                    video.put("title", title);
                    video.put("score", 1);
                    video.saveInBackground();
                } else {
                    videos.get(0).increment("score");
                    videos.get(0).saveInBackground();
                }
            }
        });

        Toast.makeText(this, R.string.share_message, Toast.LENGTH_SHORT).show();
        finish();
    }
}
