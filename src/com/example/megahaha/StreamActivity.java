package com.example.megahaha;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ShareActionProvider;

import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;

public class StreamActivity extends YouTubeFailureRecoveryActivity implements
		YouTubePlayer.PlaylistEventListener {
	private Map<Integer, String> mListOfVideos;
	private ShareActionProvider mShareActionProvider;
	private int mCurrentVideoNumber = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_stream);

		YouTubePlayerView youTubeView = (YouTubePlayerView) findViewById(R.id.youtube_view);
		youTubeView.initialize(DeveloperKey.DEVELOPER_KEY, this);

		// Get all the url of videos in playlist in right order
		new Thread() {
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
				mListOfVideos = new HashMap<Integer, String>();
				while ((pos = data.indexOf(targetString)) != -1) {
					data = data.substring(pos + targetString.length());
					String videoID = data.substring(0, 11);
					if (!mListOfVideos.containsValue(videoID)) {
						mListOfVideos.put(new Integer(nVideos), videoID);
						nVideos++;
					}
				}
			}
		}.start();

		TestDropbox();
	}

	private void TestDropbox(){
/*		FileDownload fd = api.getFileStream("dropbox", ""/public/P31.pdf", null);
		File f=new File("/sdcard/test.pdf");
		OutputStream out=new FileOutputStream(f);
		byte buf[]=new byte[1024];
		int len;

		while((len=fd.is.read(buf))>0)
		    out.write(buf,0,len);

		out.close();
		fd.is.close();*/
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
		sendIntent
				.putExtra(
						Intent.EXTRA_TEXT,
						"https://www.facebook.com/photo.php?v=119389088240096&set=vb.313150198785409&type=2&theater");
		sendIntent.setType("text/plain");
		setShareIntent(sendIntent);

		// Locate Menu Item Logout
		// MenuItem logout_item= menu.findItem(R.id.menu_item_logout);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.menu_item_Main:
			finish();
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
		}
	}

	@Override
	protected YouTubePlayer.Provider getYouTubePlayerProvider() {
		return (YouTubePlayerView) findViewById(R.id.youtube_view);

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		System.out.println("StreamActivity: onActivityResult called");
	}

	@Override
	public void onNext() {
		mCurrentVideoNumber++;
		System.out.println(mListOfVideos.get(new Integer(mCurrentVideoNumber)));
	}

	@Override
	public void onPrevious() {
		mCurrentVideoNumber--;
		System.out.println(mListOfVideos.get(new Integer(mCurrentVideoNumber)));
	}

	@Override
	public void onPlaylistEnded() {
	}
}
