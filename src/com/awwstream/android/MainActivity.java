package com.awwstream.android;

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
    private static final String YOUTUBE_PLAYLIST_ID = "PLSgXk6DxD9Qt5NB5GhfhU0apqjkr7pMyC";

    private static final String URL_TO_GET_PLAYLIST_INFORMATION =
            "https://gdata.youtube.com/feeds/api/playlists/" + YOUTUBE_PLAYLIST_ID + "?v=2&max-results=50&start-index=";

    private static final String DOCUMENT_CONTAINING_FACEBOOK_ULRS_FOR_VIDEOS =
            "https://docs.google.com/document/d/14wVSOe2vmQ4LDgqI2ZvlGtHXFCxTlGFRLlrflXa3U7I/edit?usp=sharing";

    private static final int RECOVERY_DIALOG_REQUEST = 1;

    /**
     * A list of video titles in the same order as in playlist.
     */
    private List<String> mVideoTitles = new ArrayList<String>();

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
     * A variable to count the number of pending tasks.
     */
    private int mPendingTasks;

    /**
     * A variable to keep track of the current video id.
     */
    private String mCurrentVideoId;

    /**
     * Keep track of position of the current video playing.
     */
    private int mCurrentVideoNumber;

    /**
     * Keep track of position of the first video played.
     */
    private int mFirstVideoNumber;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        // Initialize {@link YouTubePlayerView}.
        mYouTubeView = (YouTubePlayerView) findViewById(R.id.youtube_view);
        mYouTubeView.initialize(DeveloperKey.DEVELOPER_KEY, this);

        // Get {@link SharedPreferences}.
        final SharedPreferences prefs = getSharedPreferences(getString(R.string.PREFS_NAME), 0);
        mPrefEditor = prefs.edit();

        // Get current video and current time playing from {@link SharedPreferences}.
        mCurrentVideoNumber = prefs.getInt("mCurrentVideoNumber", 0);

        // Support going back up to 5 videos.
        mFirstVideoNumber = Math.max(0, mCurrentVideoNumber - 5);
    }

    private boolean isLandscape() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (mPendingTasks == 0) {
            mPendingTasks = 2;

            mVideoTitles.clear();
            mVideoIds.clear();

            // Get playlist information.
            getPlaylistInformation();

            // Get URL for each video id.
            getUrlsForVideos();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Save position of current video and current time in it.
        mPrefEditor.putInt("mCurrentVideoNumber", mCurrentVideoNumber);
        if (mYouTubePlayer != null) {
            mPrefEditor.putInt("mCurrentTimeInVideo", mYouTubePlayer.getCurrentTimeMillis());
        }
        mPrefEditor.commit();
    }

    @Override
    protected void onDestroy() {
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
                            new BufferedReader(new InputStreamReader(
                                    new URL(URL_TO_GET_PLAYLIST_INFORMATION
                                            + (mFirstVideoNumber + 1)).openStream()));

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
                mPendingTasks--;

                if (TextUtils.isEmpty(result)) {
                    return;
                }

                // With the data read from YouTube playlist, call two methods to extract video
                // titles and video ids from that data.
                getVideoTitlesFromPlaylistData(result);
                getVideoIdsFromPlaylistData(result);

                if (!TextUtils.isEmpty(mCurrentVideoId)) {
                    updateTitle(mCurrentVideoId);
                }
                
                // If both threads are done, call new method to get any video that does not get
                // linked to a Facebook URL and link it to its respective YouTube URL.
                if (mPendingTasks == 0) {
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
                mPendingTasks--;

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

                // If both threads are done, call new method to get any video that does not get
                // linked to a Facebook URL and link it to its respective YouTube URL.
                if (mPendingTasks == 0) {
                    linkVideoIdsToYoutubeUrls();
                }
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

        // After completing with mUrlMap, we update {@link ShareActionProvider} with the link for
        // currently playing video right away.
        if (!TextUtils.isEmpty(mCurrentVideoId)) {
            updateShareAction(mCurrentVideoId);
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
    private void updateTitle(String videoId) {
        final int position = mVideoIds.indexOf(videoId);
        if (0 <= position) {
            if (isLandscape()) {
                setTitle(mVideoTitles.get(position));
            } else {
                final TextView videoTitle = (TextView) findViewById(R.id.video_title);
                videoTitle.setText(mVideoTitles.get(position));
            }
        }
    }

    /**
     * Update the share action.
     */
    private void updateShareAction(String videoId) {
        final String link = mUrlMap.get(videoId);
        if (!TextUtils.isEmpty(link)) {
            if (mShareMenuItem != null) {
                ShareCompat.configureMenuItem(mShareMenuItem, ShareCompat.IntentBuilder.from(this)
                        .setText(link).setType("text/plain"));
            }
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
            mYouTubePlayer.loadPlaylist(YOUTUBE_PLAYLIST_ID, mCurrentVideoNumber, 0);
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
        // Do nothing.
    }

    @Override
    public void onAdStarted() {
        // Do nothing.
    }

    @Override
    public void onError(ErrorReason error) {
        if (ErrorReason.UNEXPECTED_SERVICE_DISCONNECTION.equals(error)) {
            mYouTubePlayer = null;
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    public void onLoaded(String videoId) {
        mCurrentVideoId = videoId;

        // Enter low profile mode.
        if (isLandscape() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mYouTubeView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }

        updateTitle(videoId);
        updateShareAction(videoId);
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
