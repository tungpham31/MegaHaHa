package com.awwstream.android;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

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
                    video.put("score", Double.valueOf(1.001));
                    video.put("like", 0);
                    video.put("dislike", 0);
                    video.put("view", 0);
                    video.put("score2", Integer.valueOf(1001));
                } else {
                    video.increment("score", 2);
                    video.increment("like");
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
                    video.increment("dislike", 1);
                    video.increment("score", -1);
                }
                video.saveInBackground();
            }
        });
    }
}
