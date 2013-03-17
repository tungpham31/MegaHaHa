package com.example.megahaha;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;

public class StreamActivity extends YouTubeFailureRecoveryActivity implements
		YouTubePlayer.PlaylistEventListener,
		YouTubePlayer.PlaybackEventListener {
	private Map<Integer, String> mListOfVideos = new HashMap<Integer, String>();
	private ShareActionProvider mShareActionProvider;
	private int mCurrentVideoNumber = 0;
	private Map<String, String> mLinkFromVideoIDToURL = new HashMap<String, String>();
	private Thread thread1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_stream);

		YouTubePlayerView youTubeView = (YouTubePlayerView) findViewById(R.id.youtube_view);
		youTubeView.initialize(DeveloperKey.DEVELOPER_KEY, this);

		// Get all the url of videos in playlist in right order
		thread1 = new Thread() {
			public void run() {
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

				String targetString = "https://www.youtube.com/v/";
				int pos;
				int nVideos = 0;
				while ((pos = data.indexOf(targetString)) != -1) {
					data = data.substring(pos + targetString.length());
					String videoID = data.substring(0, 11);
					if (!mListOfVideos.containsValue(videoID)) {
						mListOfVideos.put(new Integer(nVideos), videoID);
						nVideos++;
					}
				}
			}
		};
		thread1.start();

		new Thread() {
			public void run() {
				TestGoogleDrive();
			}
		}.start();
	}

	private void TestGoogleDrive() {
		URL fileURL = null;
		String data = "";
		try {
			fileURL = new URL(
					"https://docs.google.com/document/d/1oj2RZfVzmjMJZ9h_yHictZO9KFbBcfb7y1poFvueFiQ/edit?usp=sharing");

			BufferedReader in = null;
			in = new BufferedReader(new InputStreamReader(fileURL.openStream()));

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
			String[] values = data.substring(startPosition + 7, endPosition)
					.split(" ");
			data = data.substring(endPosition + 5);
			values[1] = refine(values[1]);
			mLinkFromVideoIDToURL.put(values[0], values[1]);
		}

		try {
			thread1.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// handle the rest videoID which does not match to a facebook URL
		Log.i("values", "" + mListOfVideos.size());
		for (int i = 0; i <= mListOfVideos.size() - 1; i++) {
			String videoID = mListOfVideos.get(new Integer(i));
			if (!mLinkFromVideoIDToURL.containsKey(videoID)) {
				String youtubeURL = "http://www.youtube.com/watch?v=" + videoID;
				mLinkFromVideoIDToURL.put(videoID, youtubeURL);
			}
		}

		// Log.i("Data", data);
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
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, "");
		sendIntent.setType("text/plain");
		setShareIntent(sendIntent);

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
	private void setShareIntent(Intent shareIntent) {
		if (mShareActionProvider != null) {
			mShareActionProvider.setShareIntent(shareIntent);
		}
	}

	@Override
	public void onInitializationSuccess(YouTubePlayer.Provider provider,
			YouTubePlayer player, boolean wasRestored) {
		if (!wasRestored) {
			player.cuePlaylist("PL4MW09z0LVvXN9Uaqg2IS0XN64CUIjfvY");
			player.setPlaylistEventListener(this);
			player.setPlaybackEventListener(this);
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
	public void onPlaylistEnded() {
	}

	@Override
	public void onBuffering(boolean isBuffering) {
	}

	@Override
	public void onPlaying() {
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		String videoID = mListOfVideos.get(new Integer(mCurrentVideoNumber));
		String videoURL = mLinkFromVideoIDToURL.get(videoID);
		sendIntent.putExtra(Intent.EXTRA_TEXT, videoURL);
		sendIntent.setType("text/plain");
		setShareIntent(sendIntent);
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
}
