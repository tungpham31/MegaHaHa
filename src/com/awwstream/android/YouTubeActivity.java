package com.awwstream.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.ShareActionProvider;
import com.actionbarsherlock.widget.ShareActionProvider.OnShareTargetSelectedListener;
import com.flurry.android.FlurryAgent;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.ErrorReason;
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener;
import com.google.android.youtube.player.YouTubePlayer.PlayerStateChangeListener;
import com.google.android.youtube.player.YouTubePlayer.PlaylistEventListener;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;

import net.simonvt.menudrawer.MenuDrawer;

/**
 * The base {@link Activity}.
 */
public abstract class YouTubeActivity extends SherlockFragmentActivity implements
        OnInitializedListener, PlaylistEventListener, PlayerStateChangeListener {
    private static final int RECOVERY_DIALOG_REQUEST = 0;

    /**
     * YouTube player.
     */
    protected YouTubePlayer mYouTubePlayer;

    /**
     * Keep track of position of the current video playing.
     */
    protected int mCurrentVideoNumber;

    /**
     * A variable to keep track of the current video id.
     */
    protected String mCurrentVideoId;

    /**
     * Title of the current video.
     */
    protected TextView mTitle;

    /**
     * The share menu item.
     */
    protected ShareActionProvider mShareActionProvider;

    /**
     * Timer for skip button.
     */
    private long mLastSkipTimeMillis;

    /**
     * The sliding {@link MenuDrawer}.
     */
    private MenuDrawer mMenuDrawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        // Initialize {@link MenuDrawer}.
        mMenuDrawer = MenuDrawer.attach(this, MenuDrawer.MENU_DRAG_WINDOW);
        mMenuDrawer.setTouchMode(MenuDrawer.TOUCH_MODE_FULLSCREEN);
        mMenuDrawer.setMenuView(R.layout.menu_drawer);

        findViewById(R.id.featured).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent = new Intent(YouTubeActivity.this, FeaturedActivity.class);
                startActivity(intent);
                finish();
            }
        });
        findViewById(R.id.hot).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent = new Intent(YouTubeActivity.this, HotActivity.class);
                startActivity(intent);
                finish();
            }
        });
        findViewById(R.id.new_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent = new Intent(YouTubeActivity.this, NewActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Initialize {@link ActionBar}.
        final View view = getLayoutInflater().inflate(R.layout.title, null);
        mTitle = (TextView) view.findViewById(R.id.title);
        getSupportActionBar().setCustomView(view);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize {@link YouTubePlayerSupportFragment}.
        ((YouTubePlayerSupportFragment) getSupportFragmentManager().findFragmentById(
                R.id.youtube_fragment)).initialize(DeveloperKey.DEVELOPER_KEY, this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        FlurryAgent.onStartSession(this, "4QVGFH2RQW3ZM5X4W2C3");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getSupportMenuInflater().inflate(R.menu.main, menu);

        // Locate {@link MenuItem} with {@link ShareActionProvider}.
        mShareActionProvider =
                (ShareActionProvider) menu.findItem(R.id.menu_item_share).getActionProvider();
        mShareActionProvider.setOnShareTargetSelectedListener(new OnShareTargetSelectedListener() {
            @Override
            public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
                FlurryAgent.logEvent("Share");

                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection.
        switch (item.getItemId()) {
            case android.R.id.home:
                mMenuDrawer.openMenu();
                return true;
            case R.id.menu_like:
                if (!TextUtils.isEmpty(mCurrentVideoId)) {
                    Utils.promoteVideo(mCurrentVideoId, mTitle.getText().toString());
                }

                Toast.makeText(this, getString(R.string.like_button_message), Toast.LENGTH_SHORT)
                        .show();
                FlurryAgent.logEvent("Like");
                return true;
            case R.id.menu_dislike:
                if (mYouTubePlayer != null
                        && System.currentTimeMillis() - mLastSkipTimeMillis >= 2000 && next()) {
                    mLastSkipTimeMillis = System.currentTimeMillis();
                }

                Toast.makeText(this, getString(R.string.dislike_button_message), Toast.LENGTH_SHORT)
                        .show();
                FlurryAgent.logEvent("Disike");
                return true;
            case R.id.menu_next:
                if (mYouTubePlayer != null
                        && System.currentTimeMillis() - mLastSkipTimeMillis >= 2000 && next()) {
                    mLastSkipTimeMillis = System.currentTimeMillis();

                    Toast.makeText(this, getString(R.string.next_button_message),
                            Toast.LENGTH_SHORT).show();
                    FlurryAgent.logEvent("Next");
                }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        FlurryAgent.onEndSession(this);
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RECOVERY_DIALOG_REQUEST) {
            // Retry initialization if user performed a recovery action and there is network
            // available.
            ((YouTubePlayerSupportFragment) getSupportFragmentManager().findFragmentById(
                    R.id.youtube_fragment)).initialize(DeveloperKey.DEVELOPER_KEY, this);
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
    public void onNext() {
        // Do nothing.
    }

    @Override
    public void onPrevious() {
        // Do nothing.
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            findViewById(R.id.content_frame).setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
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

    protected abstract void updateTitle(String videoId);

    protected abstract String getShareActionLink(String videoId);

    protected abstract boolean next();

    protected void updateShareAction(String videoId) {
        final String link = getShareActionLink(videoId);
        if (mShareActionProvider != null && !TextUtils.isEmpty(link)) {
            final Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, link);
            mShareActionProvider.setShareIntent(intent);
        }
    }
}
