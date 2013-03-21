package com.example.megahaha;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.ErrorReason;
import com.google.android.youtube.player.YouTubePlayer.PlaybackEventListener;
import com.google.android.youtube.player.YouTubePlayer.PlayerStateChangeListener;
import com.google.android.youtube.player.YouTubePlayer.PlaylistEventListener;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerView;

/**
 * The main activity.
 */
@SuppressLint("NewApi")
public final class MainActivity extends YouTubeFailureRecoveryActivity
		implements PlaylistEventListener, PlaybackEventListener,
		PlayerStateChangeListener {
	private static final String YOUTUBE_PLAYLIST_ID = "PL4MW09z0LVvXN9Uaqg2IS0XN64CUIjfvY";

	private static final String URL_TO_GET_PLAYLIST_INFORMATION = "https://gdata.youtube.com/feeds/api/playlists/PL4MW09z0LVvXN9Uaqg2IS0XN64CUIjfvY?v=2";

	private static final String DOCUMENT_CONTAINING_FACEBOOK_ULRS_FOR_VIDEOS = "https://docs.google.com/document/d/1oj2RZfVzmjMJZ9h_yHictZO9KFbBcfb7y1poFvueFiQ/edit?usp=sharing";

	/**
	 * Keep a list of video ids in the same order as in the playlist. This list
	 * is for later reference because YouTube player does not support method to
	 * get what video is currently played.
	 */
	private Map<Integer, String> mListOfVideoIDs = new HashMap<Integer, String>();

	/**
	 * ShareActionProvider is used to support users to share link of the videos
	 * in playlist. Links can be Facebook urls or YouTube urls depending on
	 * whether that particular video has Facebook url or not.
	 */
	private ShareActionProvider mShareActionProvider;

	/**
	 * Keep track of position of the current video playing.
	 */
	private int mCurrentVideoNumber = 0;

	/**
	 * Keep track of current time in playing video. Let user start video where
	 * they left off previous time.
	 */
	private int mCurrentTimeInVideo = 0;

	/**
	 * A map from a video id to its corresponding URL (either Facebook URL or
	 * YouTube URL).
	 */
	private Map<String, String> mLinkFromVideoIDToURL = new HashMap<String, String>();

	/**
	 * a list of video titles in the same order as in playlist
	 */
	private List<String> mListOfVideoTitles = new LinkedList<String>();

	/**
	 * a variable to determine whether both threads handling videoID and link
	 * url have been done. When the value is 2, it means both threads are done
	 */
	private int gotBothVideoIDAndLinkUrl = 0;

	/**
	 * Shared Preferences to save variables
	 */
	private SharedPreferences mPref;
	private SharedPreferences.Editor mPrefEditor;

	/**
	 * YouTube player.
	 */
	private YouTubePlayer mYouTubePlayer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		// Initialize YouTubePlayerView.
		final YouTubePlayerView youTubeView = (YouTubePlayerView) findViewById(R.id.youtube_view);
		youTubeView.initialize(DeveloperKey.DEVELOPER_KEY, this);

		// Get Shared Preference.
		mPref = getSharedPreferences(getString(R.string.PREFS_NAME), 0);
		mPrefEditor = mPref.edit();

		// Get current video and current time playing from Shared Preference.
		mCurrentVideoNumber = mPref.getInt("mCurrentVideoNumber", 0);
		mCurrentTimeInVideo = mPref.getInt("mCurrentTimeInVideo", 0);

		// Get playlist information
		getPlaylistInformation();

		// Get url for each videoID
		getUrlsForVideos();
	}

	/**
	 * Open a Google docs containing videoIDs and their respective urls on
	 * Facebook. Read those url and link them to respective videoID. With the
	 * rest videos which doesn't have Facebook urls, simply link them to their
	 * respective Youtube urls
	 */
	private void getUrlsForVideos() {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				// get data from the Google Doc document
				URL fileURL = null;
				String data = "";
				try {
					fileURL = new URL(
							DOCUMENT_CONTAINING_FACEBOOK_ULRS_FOR_VIDEOS);

					BufferedReader in = null;
					in = new BufferedReader(new InputStreamReader(
							fileURL.openStream()));

					// handling data from the text file
					String inputLine;
					while ((inputLine = in.readLine()) != null)
						data += inputLine;

					in.close();
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// handle the data from the text file: linking videos to their
				// corresponding facebook urls
				int startPosition = 0;
				int endPosition = 0;
				while ((startPosition = data.indexOf("/START/")) != -1) {
					endPosition = data.indexOf("/END/");
					String[] values = data.substring(startPosition + 7,
							endPosition).split(" ");
					data = data.substring(endPosition + 5);
					values[1] = refine(values[1]);
					mLinkFromVideoIDToURL.put(values[0], values[1]);
				}

				return null;
			}

			protected void onPostExecute(Void voids) {
				// increase gotBothVideoIDAndLinkUrl, indicating that one of the
				// two threads is done
				gotBothVideoIDAndLinkUrl++;

				// if both threads are done, call new method to get any video
				// that does not get linked to a facebook url and link it to its
				// respective youtube url
				if (gotBothVideoIDAndLinkUrl == 2)
					linkVideoIDToYoutubeUrl();
			}

			// refine String get from an URL
			private String refine(String s) {
				return s.replaceAll("&amp;", "&");
			}

		}.execute();
	}

	/**
	 * Open connection to an url containing information about the playlist. Read
	 * videos' title and put into mListOfVideoTitles. Read videos' id and put
	 * into mListOfVideoIDs
	 */
	private void getPlaylistInformation() {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				// read data from youtube playlist
				String data = "";
				URL youtubePlaylist;
				try {
					youtubePlaylist = new URL(URL_TO_GET_PLAYLIST_INFORMATION);
					BufferedReader in;
					in = new BufferedReader(new InputStreamReader(
							youtubePlaylist.openStream()));

					String inputLine;

					while ((inputLine = in.readLine()) != null)
						data += inputLine;

					in.close();
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				return data;
			}

			protected void onPostExecute(String result) {
				// with the data read from youtube playlist, call two methods to
				// extract video titles and video ids from that data
				getVideoTitlesFromPlaylistData(result);
				getVideoIDFromPlaylistData(result);
			}

		}.execute();
	}

	/**
	 * @param data
	 *            : youtube playlist's information extract video ids from data
	 *            and put them in order into mListOfVideoIDs
	 */
	private void getVideoIDFromPlaylistData(String data) {
		new AsyncTask<String, Void, Void>() {

			@Override
			protected Void doInBackground(String... params) {
				String data = params[0];
				String targetString = "https://www.youtube.com/v/";
				int pos;
				int nVideos = 0;
				while ((pos = data.indexOf(targetString)) != -1) {
					data = data.substring(pos + targetString.length());
					String videoID = data.substring(0, 11);
					if (!mListOfVideoIDs.containsValue(videoID)) {
						mListOfVideoIDs.put(new Integer(nVideos), videoID);
						nVideos++;
					}
				}
				return null;
			}

			protected void onPostExecute(Void myVoid) {
				// increase gotBothVideoIDAndLinkUrl to indicate that it's done
				// with getting video ids
				gotBothVideoIDAndLinkUrl++;

				// if both threads are done, call to a method to handle the job
				// left
				if (gotBothVideoIDAndLinkUrl == 2)
					linkVideoIDToYoutubeUrl();
			}

		}.execute(data);
	}

	/**
	 * @param data
	 *            : youtube playlist's information extract video titles from
	 *            data and put them in order into mListOfVideoTitles
	 */
	private void getVideoTitlesFromPlaylistData(String data) {
		new AsyncTask<String, Void, Void>() {

			@Override
			protected Void doInBackground(String... params) {
				String data = params[0];
				while (true) {
					int top = data.indexOf("<title>");
					if (top == -1)
						break;
					int bot = data.indexOf("</title>");
					String title = data.substring(top + 7, bot);
					mListOfVideoTitles.add(title);
					data = data.substring(bot + 4, data.length());
				}
				mListOfVideoTitles.remove(0); // the first title is the title of
												// the playlist, we don't need
												// it
				return null;
			}

			protected void onPostExecute(Void myVoid) {
				// set title of the currently playing video right after the
				// mListOfVideoTitles is completely built
				if (mCurrentVideoNumber < mListOfVideoTitles.size()) {
					TextView videoTitle = (TextView) findViewById(R.id.video_title);
					videoTitle.setText(mListOfVideoTitles
							.get(mCurrentVideoNumber));
				}
			}

		}.execute(data);
	}

	/**
	 * after we get facebook urls and link them to their corresponding videoIDs,
	 * there are some videoID left that does not link to any facebook urls. We
	 * simply link those to their corresponding youtube urls
	 */
	private void linkVideoIDToYoutubeUrl() {
		// handle the rest videoID which does not match to a facebook
		// URL
		for (int i = 0; i <= mListOfVideoIDs.size() - 1; i++) {
			String videoID = mListOfVideoIDs.get(new Integer(i));
			if (!mLinkFromVideoIDToURL.containsKey(videoID)) {
				String youtubeURL = "http://www.youtube.com/watch?v=" + videoID;
				mLinkFromVideoIDToURL.put(videoID, youtubeURL);
			}
		}

		// after completing with mLinkFromVideoIDToURL, we update
		// ShareActionProvider with the link for currently playing video right
		// away
		String videoID = mListOfVideoIDs.get(new Integer(mCurrentVideoNumber));
		String videoURL = mLinkFromVideoIDToURL.get(videoID);
		updateShareActionProvider(videoURL);
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		// Locate MenuItem with ShareActionProvider
		MenuItem item = menu.findItem(R.id.menu_item_share);

		// Fetch and store ShareActionProvider
		mShareActionProvider = (ShareActionProvider) item.getActionProvider();
		updateShareActionProvider(null);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		Context context = getApplicationContext();
		CharSequence text;
		Toast toast;
		int duration = Toast.LENGTH_SHORT;
		switch (item.getItemId()) {
		case R.id.menu_like:
			text = getString(R.string.like_button_message);
			toast = Toast.makeText(context, text, duration);
			toast.show();
			return true;
		case R.id.menu_dislike:
			text = getString(R.string.dislike_button_message);
			toast = Toast.makeText(context, text, duration);
			toast.show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Update the newest link for the currently playing video
	 * 
	 * @param link
	 *            : the url of the currently playing video. This url needs to be
	 *            update so that users can share it if they want to.
	 */
	@SuppressLint("NewApi")
	private void updateShareActionProvider(String link) {
		Intent shareIntent = new Intent();
		shareIntent.setAction(Intent.ACTION_SEND);
		shareIntent.putExtra(Intent.EXTRA_TEXT, link);
		shareIntent.setType("text/plain");
		if (mShareActionProvider != null) {
			mShareActionProvider.setShareIntent(shareIntent);
		}
	}

	@Override
	public void onInitializationSuccess(Provider provider,
			YouTubePlayer player, boolean wasRestored) {
		// If successfully initialize YouTube player, store that player in a
		// glocal mYouTubePlayer. Set up listeners for the player.
		mYouTubePlayer = player;
		mYouTubePlayer.setPlaylistEventListener(this);
		mYouTubePlayer.setPlaybackEventListener(this);
		mYouTubePlayer.setPlayerStateChangeListener(this);

		// If the playlist is not restored, we have load it into the youtube
		// player.
		if (!wasRestored)
			mYouTubePlayer.loadPlaylist(YOUTUBE_PLAYLIST_ID,
					mCurrentVideoNumber, mCurrentTimeInVideo);
	}

	@Override
	protected YouTubePlayer.Provider getYouTubePlayerProvider() {
		return (YouTubePlayerView) findViewById(R.id.youtube_view);
	}

	@Override
	/**
	 * when playing next video on playlist
	 */
	public void onNext() {
		mCurrentVideoNumber++;
	}

	/**
	 * when playing previous video on playlist
	 */
	@Override
	public void onPrevious() {
		mCurrentVideoNumber--;
	}

	@Override
	public void onPause() {
		super.onPause();

		// Necessary to clear first if we save preferences onPause.
		mPrefEditor.clear();
		// save position of current video and current time in it
		mPrefEditor.putInt("mCurrentVideoNumber", mCurrentVideoNumber);
		if (mYouTubePlayer != null) {
			mPrefEditor.putInt("mCurrentTimeInVideo",
					mYouTubePlayer.getCurrentTimeMillis());
		}
		mPrefEditor.commit();
	}

	@Override
	public void onLoading() {
		// Set title of the video.
		if (mCurrentVideoNumber < mListOfVideoTitles.size()) {
			TextView videoTitle = (TextView) findViewById(R.id.video_title);
			videoTitle.setText(mListOfVideoTitles.get(mCurrentVideoNumber));
		}

		// Update the link to share for this currently playing video.
		final String videoID = mListOfVideoIDs.get(new Integer(
				mCurrentVideoNumber));
		final String videoURL = mLinkFromVideoIDToURL.get(videoID);
		updateShareActionProvider(videoURL);
	}

	@Override
	public void onPlaylistEnded() {
		// Do nothing.
	}

	@Override
	public void onBuffering(boolean isBuffering) {
		// Do nothing.
	}

	@Override
	public void onPlaying() {
		// Do nothing.
	}

	@Override
	public void onSeekTo(int newPositionMillis) {
		// Do nothing.
	}

	@Override
	public void onStopped() {
		// Do nothing.
	}

	@Override
	public void onPaused() {
		// Do nothing.
	}

	@Override
	public void onAdStarted() {
		// Do nothing.
	}

	@Override
	public void onError(ErrorReason error) {
		// Do nothing.
	}

	@Override
	public void onLoaded(String arg0) {
		// Do nothing.
	}

	@Override
	public void onVideoEnded() {
		// Do nothing.
	}

	@Override
	public void onVideoStarted() {
		// Do nothing.
	}
}
