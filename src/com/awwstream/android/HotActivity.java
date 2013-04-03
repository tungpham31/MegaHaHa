package com.awwstream.android;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.youtube.player.YouTubePlayerSupportFragment;
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

        setContentView(R.layout.main);

        // Initialize {@link ActionBar}.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            final View view = getLayoutInflater().inflate(R.layout.title, null);
            mTitle = (TextView) view.findViewById(R.id.title);
            getSupportActionBar().setCustomView(view);
        }

        // Initialize {@link YouTubePlayerSupportFragment}.
        ((YouTubePlayerSupportFragment) getSupportFragmentManager().findFragmentById(
                R.id.youtube_fragment)).initialize(DeveloperKey.DEVELOPER_KEY, this);

        final ParseQuery query = new ParseQuery("Video");
        query.addDescendingOrder("score");
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
        final ParseObject video = mVideos.get(mCurrentVideoNumber);
        mYouTubePlayer.loadVideo(video.getString("videoId"));
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
}
