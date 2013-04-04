package com.awwstream.android;

import android.app.Activity;
import android.os.Bundle;

import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;

/**
 * The {@link Activity} to play Hot videos.
 */
public class HotActivity extends YouTubeActivity {
    private List<ParseObject> mVideos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ParseQuery innerQuery = new ParseQuery("Watched");
        innerQuery.whereEqualTo("username", mPref.getString("username", null));

        final ParseQuery query = new ParseQuery("Video");
        query.whereDoesNotMatchKeyInQuery("videoId", "videoId", innerQuery);
        query.addDescendingOrder(sortKey());
        query.findInBackground(new FindCallback() {
            @Override
            public void done(List<ParseObject> videos, ParseException e) {
                mVideos = videos;

                if (mVideos != null && !mVideos.isEmpty()) {
                    mCurrentVideoNumber = 0;
                    loadVideo();
                }
            }
        });
    }

    private void loadVideo() {
        if (mYouTubePlayer == null || mVideos == null || mVideos.isEmpty()) {
            return;
        }

        final ParseObject video = mVideos.get(mCurrentVideoNumber);
        mYouTubePlayer.loadVideo(video.getString("videoId"));
    }

    @Override
    public void
            onInitializationSuccess(Provider provider, YouTubePlayer player, boolean wasRestored) {
        super.onInitializationSuccess(provider, player, wasRestored);

        if (!wasRestored) {
            loadVideo();
        }
    }

    @Override
    public void onVideoEnded() {
        next();
    }

    @Override
    protected void updateTitle(String videoId) {
        mTitle.setText(mVideos.get(mCurrentVideoNumber).getString("title"));
    }

    @Override
    protected String getShareActionLink(String videoId) {
        return "http://www.youtube.com/watch?v=" + videoId;
    }

    @Override
    protected boolean next() {
        if (mCurrentVideoNumber < mVideos.size() - 1) {
            mCurrentVideoNumber++;
            loadVideo();
            return true;
        } else {
            return false;
        }
    }

    protected String sortKey() {
        return "score";
    }
}
