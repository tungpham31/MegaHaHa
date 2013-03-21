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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.ErrorReason;
import com.google.android.youtube.player.YouTubePlayerView;

@SuppressLint("NewApi")
public class StreamActivity extends YouTubeFailureRecoveryActivity implements
		YouTubePlayer.PlaylistEventListener,
		YouTubePlayer.PlaybackEventListener,
		YouTubePlayer.PlayerStateChangeListener {
	private Map<Integer, String> mListOfVideoIDs = new HashMap<Integer, String>();
	private ShareActionProvider mShareActionProvider;
	private int mCurrentVideoNumber = 0;
	private int mCurrentTimeInVideo = 0;
	private Map<String, String> mLinkFromVideoIDToURL = new HashMap<String, String>();
	private List<String> mListOfVideoTitles = new LinkedList<String>();
	private int gotBothVideoIDAndLinkUrl = 0;
	private SharedPreferences mPref;
	private SharedPreferences.Editor mPrefEditor;
	private YouTubePlayer youtubePlayer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_stream);

		// initialize youtubeplayerview
		YouTubePlayerView youTubeView = (YouTubePlayerView) findViewById(R.id.youtube_view);
		youTubeView.initialize(DeveloperKey.DEVELOPER_KEY, this);

		// get Shared Preference
		mPref = getSharedPreferences(getString(R.string.PREFS_NAME), 0);
		mPrefEditor = mPref.edit();

		// get current video and current time playing from Shared Preference
		mCurrentVideoNumber = mPref.getInt("mCurrentVideoNumber", 0);
		mCurrentTimeInVideo = mPref.getInt("mCurrentTimeInVideo", 0);

		// Get videos' ids and titles
		getPlaylistInformation();

		// Get url for each videoID
		getUrlsForVideos();
	}

	/*
	 * Open a Google docs containing videoIDs and their respective urls on
	 * Facebook. Read those url and link them to respective videoID With the
	 * rest videos which doesn't have Facebook urls, simply link them to their
	 * respective Youtube url
	 */
	private void getUrlsForVideos() {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... params) {
				URL fileURL = null;
				String data = "";
				try {
					fileURL = new URL(
							"https://docs.google.com/document/d/1oj2RZfVzmjMJZ9h_yHictZO9KFbBcfb7y1poFvueFiQ/edit?usp=sharing");

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

				// handle data get from the text file
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
				gotBothVideoIDAndLinkUrl++;
				if (gotBothVideoIDAndLinkUrl == 2)
					linkVideoIDToYoutubeUrl();
				Log.i("Thread", "finish getUrlsForVideos");
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
				// TODO Auto-generated method stub
				// get data about youtube playlist
				String data = "";
				URL youtubePlaylist;
				try {
					youtubePlaylist = new URL(
							"https://gdata.youtube.com/feeds/api/playlists/PL4MW09z0LVvXN9Uaqg2IS0XN64CUIjfvY?v=2");
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
				getVideoTitlesFromPlaylistData(result);
				getVideoIDFromPlaylistData(result);
				Log.i("Thread", "Finish getPlaylistInformation");
			}

		}.execute();
	}

	/**
	 * @param data
	 * @return
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
				gotBothVideoIDAndLinkUrl++;
				if (gotBothVideoIDAndLinkUrl == 2)
					linkVideoIDToYoutubeUrl();
			}

		}.execute(data);
	}

	/**
	 * @param data
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

		}.execute(data);
	}

	/**
	 * 
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

	// refine String get from an URL
	private String refine(String s) {
		return s.replaceAll("&amp;", "&");
	}

	@SuppressLint("NewApi")
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.stream, menu);

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
			text = "You like this video";
			toast = Toast.makeText(context, text, duration);
			toast.show();
			return true;
		case R.id.menu_dislike:
			text = "You dislike this video";
			toast = Toast.makeText(context, text, duration);
			toast.show();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	// Call to update the share intent
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
	public void onInitializationSuccess(YouTubePlayer.Provider provider,
			YouTubePlayer player, boolean wasRestored) {
		if (!wasRestored) {
			youtubePlayer = player;
			youtubePlayer.loadPlaylist("PL4MW09z0LVvXN9Uaqg2IS0XN64CUIjfvY",
					mCurrentVideoNumber, mCurrentTimeInVideo);
			youtubePlayer.setPlaylistEventListener(this);
			youtubePlayer.setPlaybackEventListener(this);
			youtubePlayer.setPlayerStateChangeListener(this);
		}
	}

	@Override
	protected YouTubePlayer.Provider getYouTubePlayerProvider() {
		return (YouTubePlayerView) findViewById(R.id.youtube_view);

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
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
	/*
	 * on activity paused
	 */
	protected void onPause() {
		System.out.println(youtubePlayer.getCurrentTimeMillis());
		mPrefEditor.putInt("mCurrentVideoNumber", mCurrentVideoNumber);
		mPrefEditor.putInt("mCurrentTimeInVideo",
				youtubePlayer.getCurrentTimeMillis());
		mPrefEditor.commit();
		super.onPause();
	}

	@Override
	public void onPlaylistEnded() {
	}

	@Override
	public void onBuffering(boolean isBuffering) {
	}

	@Override
	public void onPlaying() {
	}

	@Override
	public void onSeekTo(int newPositionMillis) {
	}

	@Override
	public void onStopped() {
	}

	@Override
	public void onPaused() {
		// TODO Auto-generated method stub
	}

	@Override
	public void onAdStarted() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onError(ErrorReason arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onLoaded(String arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onLoading() {
		// set title of the video
		if (mCurrentVideoNumber < mListOfVideoTitles.size()) {
			TextView videoTitle = (TextView) findViewById(R.id.video_title);
			videoTitle.setText(mListOfVideoTitles.get(mCurrentVideoNumber));
		}

		// update the link to share for this currently playing video
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		String videoID = mListOfVideoIDs.get(new Integer(mCurrentVideoNumber));
		String videoURL = mLinkFromVideoIDToURL.get(videoID);
		updateShareActionProvider(videoURL);
	}

	@Override
	public void onVideoEnded() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onVideoStarted() {
		// TODO Auto-generated method stub

	}
}
