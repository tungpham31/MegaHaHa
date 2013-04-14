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
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.List;

/**
 * The {@link Activity} to play user-generated content.
 */
public abstract class UserActivity extends YouTubeActivity {
    private List<ParseObject> mVideos;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setProgressBarIndeterminateVisibility(true);

        if (mPref.getString("username", null) == null) {
            ParseUser.enableAutomaticUser();
            ParseUser.getCurrentUser().saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (isFinishing()) {
                        return;
                    }

                    mPref.edit().putString("username", ParseUser.getCurrentUser().getUsername())
                            .commit();
                    loadVideos(savedInstanceState != null);
                }
            });
        } else {
            loadVideos(savedInstanceState != null);
        }
    }

    private void loadVideos(final boolean wasRestored) {
        final ParseQuery watchedQuery = new ParseQuery("Watched");
        watchedQuery.whereEqualTo("username", mPref.getString("username", null));
        watchedQuery.addDescendingOrder("createdAt");
        watchedQuery.setLimit(1000);

        final ParseQuery notWatchedQuery = new ParseQuery("Video");
        notWatchedQuery.whereDoesNotMatchKeyInQuery("videoId", "videoId", watchedQuery);
        notWatchedQuery.addDescendingOrder("createdAt");
        notWatchedQuery.setLimit(1000);

        final ParseQuery featureQuery = new ParseQuery("FeaturedVideo");
        featureQuery.whereLessThan("position", mPref.getInt("mCurrentVideoNumber", 0));

        final ParseQuery query = new ParseQuery("Video");
        query.whereMatchesKeyInQuery("videoId", "videoId", notWatchedQuery);
        query.whereDoesNotMatchKeyInQuery("videoId", "videoID", featureQuery);
        query.addDescendingOrder(sortKey());
        query.addDescendingOrder("createdAt");
        query.findInBackground(new FindCallback() {
            @Override
            public void done(List<ParseObject> videos, ParseException e) {
                if (isFinishing()) {
                    return;
                }

                setProgressBarIndeterminateVisibility(false);

                mVideos = videos;

                if (mVideos != null && !mVideos.isEmpty()) {
                    mCurrentVideoNumber = 0;
                    mCurrentVideoId = mVideos.get(0).getString("videoId");

                    updateTitle(mCurrentVideoId);
                    updateShareAction(mCurrentVideoId);

                    if (!wasRestored) {
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
    
    protected void updateNumberOfViews(ParseObject currentVideo){
        currentVideo.increment("view", Integer.valueOf(1));
        currentVideo.saveInBackground();
    }

    @Override
    protected void updateTitle(String videoId) {
        if (mVideos == null || mVideos.isEmpty()) {
            return;
        }

        mTitle.setText(mVideos.get(mCurrentVideoNumber).getString("title"));
    }

    @Override
    protected String getShareActionLink(String videoId) {
        return "http://www.youtube.com/watch?v=" + videoId;
    }

    @Override
    protected boolean next() {
        if (mVideos == null || mVideos.isEmpty()) {
            return false;
        }

        if (mCurrentVideoNumber < mVideos.size() - 1) {
            markVideoAsWatched(mCurrentVideoId);
            updateNumberOfViews(mVideos.get(mCurrentVideoNumber));

            mCurrentVideoNumber++;
            mCurrentVideoId = mVideos.get(mCurrentVideoNumber).getString("videoId");

            updateTitle(mCurrentVideoId);
            updateShareAction(mCurrentVideoId);
            updateLikeItem(false);

            loadVideo();
            return true;
        } else {
            return false;
        }
    }

    protected abstract String sortKey();
}
