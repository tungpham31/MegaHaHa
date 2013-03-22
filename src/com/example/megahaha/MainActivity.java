package com.example.megahaha;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.ErrorReason;
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener;
import com.google.android.youtube.player.YouTubePlayer.PlayerStateChangeListener;
import com.google.android.youtube.player.YouTubePlayer.PlaylistEventListener;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The main activity.
 */
public final class MainActivity extends YouTubeBaseActivity implements OnInitializedListener,
        PlaylistEventListener, PlayerStateChangeListener {
    private static final String YOUTUBE_PLAYLIST_ID = "PL4MW09z0LVvXN9Uaqg2IS0XN64CUIjfvY";

    private static final String URL_TO_GET_PLAYLIST_INFORMATION =
            "https://gdata.youtube.com/feeds/api/playlists/PL4MW09z0LVvXN9Uaqg2IS0XN64CUIjfvY?v=2";

    private static final String DOCUMENT_CONTAINING_FACEBOOK_ULRS_FOR_VIDEOS =
            "https://docs.google.com/document/d/1oj2RZfVzmjMJZ9h_yHictZO9KFbBcfb7y1poFvueFiQ/edit?usp=sharing";

    private static final int RECOVERY_DIALOG_REQUEST = 1;

    /**
     * Keep a list of video ids in the same order as in the playlist. This list is for later
     * reference because YouTube player does not support method to get what video is currently
     * played.
     */
    private List<String> mVideoIds = new ArrayList<String>();

    /**
     * A map from a video id to its corresponding URL (either Facebook URL or YouTube URL).
     */
    private Map<String, String> mUrlMap = new HashMap<String, String>();

    /**
     * A list of video titles in the same order as in playlist.
     */
    private List<String> mVideoTitles = new ArrayList<String>();

    /**
     * A variable to determine whether both threads handling video id and link URL have finished.
     * When the value is 2, it means both threads are done
     */
    private int mGotBothVideoIdsAndLinkUrls = 0;

    /**
     * Keep track of position of the current video playing.
     */
    private int mCurrentVideoNumber = 0;

    /**
     * Keep track of current time in playing video. Let user start video where they left off
     * previous time.
     */
    private int mCurrentTimeInVideo = 0;

    /**
     * The share menu item.
     */
    private MenuItem mShareMenuItem;

    /**
     * Shared Preferences to save variables.
     */
    private Editor mPrefEditor;

    /**
     * YouTube player.
     */
    private YouTubePlayerView mYouTubeView;
    private YouTubePlayer mYouTubePlayer;

    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        // Initialize {@link YouTubePlayerView}.
        mYouTubeView = (YouTubePlayerView) findViewById(R.id.youtube_view);
        mYouTubeView.initialize(DeveloperKey.DEVELOPER_KEY, this);

        // Get {@link SharedPreferences}.
        final SharedPreferences prefs = getSharedPreferences(getString(R.string.PREFS_NAME), 0);
        mPrefEditor = prefs.edit();

        // Get current video and current time playing from Shared Preference.
        mCurrentVideoNumber = prefs.getInt("mCurrentVideoNumber", 0);
        mCurrentTimeInVideo = prefs.getInt("mCurrentTimeInVideo", 0);
        if (mCurrentTimeInVideo < 0) {
            mCurrentTimeInVideo = 0;
        }

        // Get playlist information.
        getPlaylistInformation();

        // Get URL for each video id.
        getUrlsForVideos();
    }

    private boolean isLandscape() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    @Override
    public void onPause() {
        super.onPause();

        // Save position of current video and current time in it.
        mPrefEditor.putInt("mCurrentVideoNumber", mCurrentVideoNumber);
        if (mYouTubePlayer != null) {
            mPrefEditor.putInt("mCurrentTimeInVideo", mYouTubePlayer.getCurrentTimeMillis());
        }
        mPrefEditor.commit();
    }

    @Override
    public void onDestroy() {
        if (mYouTubePlayer != null) {
            mYouTubePlayer.setPlaylistEventListener(this);
            mYouTubePlayer.setPlayerStateChangeListener(this);
            mYouTubePlayer = null;
        }

        super.onDestroy();
    }

    /**
     * Opens connection to an URL containing information about the playlist. Read video titles and
     * put into mVideoTitles. Read videos' id and put into mVideoIds
     */
    private void getPlaylistInformation() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                // Read data from YouTube playlist.
                BufferedReader reader = null;
                try {
                    reader =
                            new BufferedReader(new InputStreamReader(new URL(
                                    URL_TO_GET_PLAYLIST_INFORMATION).openStream()));

                    final StringBuilder builder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }

                    return builder.toString();
                } catch (Exception e) {
                    return null;
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Exception e) {
                            // Do nothing.
                        }
                    }
                }
            }

            protected void onPostExecute(String result) {
                if (TextUtils.isEmpty(result)) {
                    return;
                }

                // With the data read from YouTube playlist, call two methods to extract video
                // titles and video ids from that data.
                getVideoTitlesFromPlaylistData(result);
                getVideoIdsFromPlaylistData(result);

                // Increase mGotBothVideoIdsAndLinkUrls, indicating that one of the two threads
                // is done.
                mGotBothVideoIdsAndLinkUrls++;

                // If both threads are done, call new method to get any video that does not get
                // linked to a Facebook URL and link it to its respective YouTube URL.
                if (mGotBothVideoIdsAndLinkUrls == 2) {
                    linkVideoIdsToYoutubeUrls();
                }
            }
        }.execute();
    }

    /**
     * Extract video titles from data and put them in order into mVideoTitles.
     * @param data
     *            : YouTube playlist's information
     */
    private void getVideoTitlesFromPlaylistData(String data) {
        while (true) {
            int top = data.indexOf("<title>");
            if (top == -1) {
                break;
            }
            int bot = data.indexOf("</title>");
            mVideoTitles.add(data.substring(top + 7, bot));
            data = data.substring(bot + 4, data.length());
        }

        mVideoTitles.remove(0); // the first title is the title of the playlist, we don't need it.
        updateTitle();
    }

    /**
     * Extracts video ids from data and put them in order into mVideoIds.
     * @param data
     *            : YouTube playlist's information
     */
    private void getVideoIdsFromPlaylistData(String data) {
        final String targetString = "https://www.youtube.com/v/";
        int pos;

        while ((pos = data.indexOf(targetString)) != -1) {
            data = data.substring(pos + targetString.length());
            final String videoId = data.substring(0, 11);
            if (!mVideoIds.contains(videoId)) {
                mVideoIds.add(videoId);
            }
        }
    }

    /**
     * Open a Google Doc containing video ids and their respective URLs on Facebook. Read those URLs
     * and link them to respective video ids. With the rest videos which doesn't have Facebook URLs,
     * simply link them to their respective YouTube URLs.
     */
    private void getUrlsForVideos() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                // Get data from the Google Doc document.
                BufferedReader reader = null;
                try {
                    reader =
                            new BufferedReader(new InputStreamReader(new URL(
                                    DOCUMENT_CONTAINING_FACEBOOK_ULRS_FOR_VIDEOS).openStream()));

                    // Handling data from the text file.
                    final StringBuilder builder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line);
                    }

                    return builder.toString();
                } catch (Exception e) {
                    return null;
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (Exception e) {
                            // Do nothing.
                        }
                    }
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (TextUtils.isEmpty(result)) {
                    return;
                }

                // Handle the data from the text file: linking videos to their corresponding
                // Facebook URLs.
                int startPosition = 0;
                int endPosition = 0;
                while ((startPosition = result.indexOf("/START/")) != -1) {
                    endPosition = result.indexOf("/END/");
                    final String[] values =
                            result.substring(startPosition + 7, endPosition).split(" ");
                    result = result.substring(endPosition + 5);
                    mUrlMap.put(values[0], refine(values[1]));
                }

                // Increase gotBothVideoIDAndLinkUrl, indicating that one of the two threads is done
                mGotBothVideoIdsAndLinkUrls++;

                // If both threads are done, call new method to get any video that does not get
                // linked to a Facebook URL and link it to its respective YouTube URL.
                if (mGotBothVideoIdsAndLinkUrls == 2)
                    linkVideoIdsToYoutubeUrls();
            }

            /**
             * Refines String get from an URL.
             */
            private String refine(String s) {
                return s.replaceAll("&amp;", "&");
            }

        }.execute();
    }

    /**
     * After we get Facebook URLs and link them to their corresponding video ids, there are some
     * video id left that does not link to any Facebook URLs. We simply link those to their
     * corresponding YouTube URLs
     */
    private void linkVideoIdsToYoutubeUrls() {
        // Handle the rest video ids which does not match to a Facebook URL.
        for (String videoId : mVideoIds) {
            if (!mUrlMap.containsKey(videoId)) {
                mUrlMap.put(videoId, "http://www.youtube.com/watch?v=" + videoId);
            }
        }

        // After completing with mLinkFromVideoIDToURL, we update {@link ShareActionProvider} with
        // the link for currently playing video right away.
        if (mCurrentVideoNumber < mVideoIds.size()) {
            final String videoId = mVideoIds.get(mCurrentVideoNumber);
            updateShareActionProvider(mUrlMap.get(videoId));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        // Locate MenuItem with ShareActionProvider.
        mShareMenuItem = menu.findItem(R.id.menu_item_share);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection.
        switch (item.getItemId()) {
            case R.id.menu_like:
                Toast.makeText(this, getString(R.string.like_button_message), Toast.LENGTH_SHORT)
                        .show();
                return true;
            case R.id.menu_dislike:
                Toast.makeText(this, getString(R.string.dislike_button_message), Toast.LENGTH_SHORT)
                        .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Update the title of the video.
     */
    private void updateTitle() {
        // Set title of the video.
        if (mCurrentVideoNumber < mVideoTitles.size()) {
            final TextView videoTitle = (TextView) findViewById(R.id.video_title);
            if (isLandscape()) {
                videoTitle.setVisibility(View.GONE);
                setTitle(mVideoTitles.get(mCurrentVideoNumber));
            } else {
                videoTitle.setText(mVideoTitles.get(mCurrentVideoNumber));
            }
        }
    }

    /**
     * Update the newest link for the currently playing video
     * @param link
     *            : the url of the currently playing video. This url needs to be update so that
     *            users can share it if they want to.
     */
    private void updateShareActionProvider(String link) {
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, link);
        intent.setType("text/plain");
        if (mShareMenuItem != null) {
            ShareCompat.configureMenuItem(
                    mShareMenuItem,
                    ShareCompat.IntentBuilder.from(MainActivity.this).setText(link)
                            .setType("text/plain"));
        }
    }

    @Override
    public void
            onInitializationSuccess(Provider provider, YouTubePlayer player, boolean wasRestored) {
        // If successfully initialize YouTube player, store that player in a global mYouTubePlayer.
        // Set up listeners for the player.
        mYouTubePlayer = player;
        mYouTubePlayer.setPlaylistEventListener(this);
        mYouTubePlayer.setPlayerStateChangeListener(this);

        // If the playlist is not restored, we have load it into the YouTube player.
        if (!wasRestored)
            mYouTubePlayer.loadPlaylist(YOUTUBE_PLAYLIST_ID, mCurrentVideoNumber,
                    mCurrentTimeInVideo);
    }

    @Override
    public void onNext() {
        mCurrentVideoNumber++;
    }

    @Override
    public void onPrevious() {
        mCurrentVideoNumber--;
    }

    @Override
    public void onPlaylistEnded() {
        // Do nothing.
    }

    @Override
    public void onLoading() {
        updateTitle();

        // Update the link to share for this currently playing video.
        if (mCurrentVideoNumber < mVideoIds.size()) {
            final String videoId = mVideoIds.get(mCurrentVideoNumber);
            updateShareActionProvider(mUrlMap.get(videoId));
        }
    }

    @Override
    public void onAdStarted() {
        // Do nothing.
    }

    @Override
    public void onError(ErrorReason error) {
        if (ErrorReason.UNEXPECTED_SERVICE_DISCONNECTION.equals(error)) {
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void onLoaded(String videoId) {
        // Enter low profile mode.
        if (isLandscape() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mYouTubeView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
    }

    @Override
    public void onVideoEnded() {
        // Do nothing.
    }

    @Override
    public void onVideoStarted() {
        // Do nothing.
    }

    @Override
    public void onInitializationFailure(Provider provider, YouTubeInitializationResult errorReason) {
        if (errorReason.isUserRecoverableError()) {
            errorReason.getErrorDialog(this, RECOVERY_DIALOG_REQUEST).show();
        } else {
            final String errorMessage =
                    String.format(getString(R.string.error_player), errorReason.toString());
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RECOVERY_DIALOG_REQUEST) {
            // Retry initialization if user performed a recovery action and there is network
            // available.
            ((YouTubePlayerView) findViewById(R.id.youtube_view)).initialize(
                    DeveloperKey.DEVELOPER_KEY, this);
        }
    }
}
