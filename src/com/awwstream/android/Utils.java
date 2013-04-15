package com.awwstream.android;

import com.parse.FunctionCallback;
import com.parse.GetCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.HashMap;

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
        query.getFirstInBackground(new GetCallback() {
            @Override
            public void done(ParseObject video, ParseException e) {
                if (video == null) {
                    video = new ParseObject("Video");
                    video.put("videoId", videoId);
                    video.put("title", title);
                    video.put("score", 0);
                    video.put("like", 0);
                    video.put("view", 0);
                    video.put("score2", 0);
                } else {
                    video.increment("score", Integer.valueOf(2));
                    video.increment("like", Integer.valueOf(1));
                }
                video.saveInBackground();
            }
        });
    }

    /**
     * Demotes a video.
     * @param videoId
     *            the video id.
     * @param title
     *            the title.
     */
    public static void demoteVideo(final String videoId) {
        final ParseQuery query = new ParseQuery("Video");
        query.whereEqualTo("videoId", videoId);
        query.getFirstInBackground(new GetCallback() {
            @Override
            public void done(ParseObject video, ParseException e) {
                if (video != null) {
                    video.increment("score", Integer.valueOf(-1));
                }
                video.saveInBackground();
            }
        });
    }
}
