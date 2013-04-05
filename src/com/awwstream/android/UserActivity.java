package com.awwstream.android;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;

import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;

/**
 * The {@link Activity} to play user-generated content.
 */
public abstract class UserActivity extends YouTubeActivity {
    private List<ParseObject> mVideos;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ParseQuery watchedQuery = new ParseQuery("Watched");
        watchedQuery.whereEqualTo("username", mPref.getString("username", null));

        final ParseQuery notWatchedQuery = new ParseQuery("Video");
        notWatchedQuery.whereDoesNotMatchKeyInQuery("videoId", "videoId", watchedQuery);

        final ParseQuery featureQuery = new ParseQuery("FeaturedVideo");
        featureQuery.whereLessThan("position", mPref.getInt("mCurrentVideoNumber", 0));

        final ParseQuery query = new ParseQuery("Video");
        query.whereMatchesKeyInQuery("videoId", "videoId", notWatchedQuery);
        query.whereDoesNotMatchKeyInQuery("videoId", "videoId", featureQuery);
        query.addDescendingOrder(sortKey());
        query.addDescendingOrder("createdAt");
        query.findInBackground(new FindCallback() {
            @Override
            public void done(List<ParseObject> videos, ParseException e) {
                mVideos = videos;

                if (mVideos != null && !mVideos.isEmpty()) {
                    mCurrentVideoNumber = 0;
                    mCurrentVideoId = mVideos.get(0).getString("videoId");

                    updateTitle(mCurrentVideoId);
                    updateShareAction(mCurrentVideoId);

                    if (savedInstanceState == null) {
                        loadVideo();
                    }
                }
            }
        });
    }

    private void loadVideo() {
        if (mYouTubePlayer == null || TextUtils.isEmpty(mCurrentVideoId)) {
            return;
        }

        mYouTubePlayer.loadVideo(mCurrentVideoId);
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
        if (mVideos != null) {
            mTitle.setText(mVideos.get(mCurrentVideoNumber).getString("title"));
        }
    }

    @Override
    protected String getShareActionLink(String videoId) {
        return "http://www.youtube.com/watch?v=" + videoId;
    }

    @Override
    protected boolean next() {
        if (mCurrentVideoNumber < mVideos.size() - 1) {
            markVideoAsWatched(mCurrentVideoId);

            mCurrentVideoNumber++;
            mCurrentVideoId = mVideos.get(0).getString("videoId");

            updateTitle(mCurrentVideoId);
            updateShareAction(mCurrentVideoId);

            loadVideo();
            return true;
        } else {
            return false;
        }
    }

    protected abstract String sortKey();
}
