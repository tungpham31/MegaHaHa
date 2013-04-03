package com.awwstream.android;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;

/**
 * Helper class.
 */
public class Utils {
    private Utils() {
        // Do nothing.
    }

    /**
     * Promotes a video.
     * @param videoId
     *            the video id.
     * @param title
     *            the title.
     */
    public static void promoteVideo(final String videoId, final String title) {
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
    }
}
