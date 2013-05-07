package com.awwstream.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.actionbarsherlock.widget.ShareActionProvider;
import com.actionbarsherlock.widget.ShareActionProvider.OnShareTargetSelectedListener;
import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.RequestAsyncTask;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.Session.NewPermissionsRequest;
import com.facebook.Session.StatusCallback;
import com.facebook.SessionState;
import com.flurry.android.FlurryAgent;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayer.ErrorReason;
import com.google.android.youtube.player.YouTubePlayer.OnInitializedListener;
import com.google.android.youtube.player.YouTubePlayer.PlayerStateChangeListener;
import com.google.android.youtube.player.YouTubePlayer.PlaylistEventListener;
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.revmob.RevMob;
import com.revmob.ads.fullscreen.RevMobFullscreen;

import net.simonvt.menudrawer.MenuDrawer;
import net.simonvt.menudrawer.MenuDrawer.OnDrawerStateChangeListener;

import java.util.Arrays;

/**
 * The base {@link Activity}.
 */
public abstract class YouTubeActivity extends SherlockFragmentActivity implements
        OnInitializedListener, PlaylistEventListener, PlayerStateChangeListener {
    private static final int RECOVERY_DIALOG_REQUEST = 0;

    /**
     * The sliding {@link MenuDrawer}.
     */
    private MenuDrawer mMenuDrawer;

    /**
     * Title of the current video.
     */
    protected TextView mTitle;

    /**
     * The share menu item.
     */
    protected ShareActionProvider mShareActionProvider;

    /**
     * The like menu item.
     */
    protected MenuItem mLikeItem;

    /**
     * Keep track of the state of like menu item.
     */
    protected boolean mIsLikeItemSelected;

    /**
     * The Facebook Connect Item.
     */
    protected MenuItem mFacebookConnectItem;

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
     * {@link SharedPreferences} to save variables.
     */
    protected SharedPreferences mPref;

    /**
     * Timer for skip button.
     */
    private long mLastSkipTimeMillis;

    /**
     * Instance to manage RevMob ad.
     */
    private RevMob revmob;

    /**
     * Instance of RevMob fullscreen ad.
     */
    private RevMobFullscreen fullScreenAd;

    private boolean mAdShown = false;
    private boolean mShouldQuitAutomatically = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkAndUpdateApp();

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        // Initialize {@link ActionBar}.
        final View view = getLayoutInflater().inflate(R.layout.title, null);
        mTitle = (TextView) view.findViewById(R.id.title);
        getSupportActionBar().setCustomView(view);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Initialize {@link MenuDrawer}.
        mMenuDrawer = MenuDrawer.attach(this, MenuDrawer.MENU_DRAG_CONTENT);
        mMenuDrawer.setTouchMode(MenuDrawer.TOUCH_MODE_FULLSCREEN);
        mMenuDrawer.setContentView(R.layout.main);
        mMenuDrawer.setMenuView(R.layout.menu_drawer);
        mMenuDrawer.setOnDrawerStateChangeListener(new OnDrawerStateChangeListener() {
            @Override
            public void onDrawerStateChange(int oldState, int newState) {
                if (newState == MenuDrawer.STATE_CLOSED || newState == MenuDrawer.STATE_CLOSING) {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                } else {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                }
            }
        });
        mMenuDrawer.setMenuSize(getResources().getDimensionPixelSize(R.dimen.menu_drawer_width));

        findViewById(R.id.hot).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent = new Intent(YouTubeActivity.this, HotActivity.class);
                intent.putExtra("internal", true);
                startActivity(intent);
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

                FlurryAgent.logEvent("Hot");
                EasyTracker.getTracker().sendEvent("Page", "View", "Hot", null);
            }
        });
        findViewById(R.id.new_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent intent = new Intent(YouTubeActivity.this, NewActivity.class);
                intent.putExtra("internal", true);
                startActivity(intent);
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

                FlurryAgent.logEvent("New");
                EasyTracker.getTracker().sendEvent("Page", "View", "New", null);
            }
        });

        // Initialize {@link YouTubePlayerSupportFragment}.
        ((YouTubePlayerSupportFragment) getSupportFragmentManager().findFragmentById(
                R.id.youtube_fragment)).initialize(DeveloperKey.DEVELOPER_KEY, this);

        // Get {@link SharedPreferences}.
        mPref = getSharedPreferences(getString(R.string.PREFS_NAME), 0);

        final int launchCount = mPref.getInt("launchCount", 0);
        if (!getIntent().getBooleanExtra("internal", false) && launchCount < 3) {
            mMenuDrawer.openMenu(false);
            mPref.edit().putInt("launchCount", launchCount + 1).commit();
        }

        // Schedule an alarm in 2 weeks.
        Utils.setAlarm(getApplicationContext(), 14);

        // Start RevMob and preload fullscreen ad.
        revmob = RevMob.start(this, "51886e8589c9d9a60200009b");
        fullScreenAd = revmob.createFullscreen(this, null);
    }

    @Override
    protected void onStart() {
        super.onStart();

        FlurryAgent.onStartSession(this, "4QVGFH2RQW3ZM5X4W2C3");
        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getSupportMenuInflater().inflate(R.menu.main, menu);

        // Locate like menu item.
        mLikeItem = (MenuItem) menu.findItem(R.id.menu_like);

        // Set state of like menu item to not selected
        mIsLikeItemSelected = false;

        // Locate {@link menu_fb_connect
        mFacebookConnectItem = menu.findItem(R.id.menu_fb_connect);
        if (hasUserAndFacebookPermissionToPublishToFacebook())
            mFacebookConnectItem.setTitle(getString(R.string.fb_disconnect_button));
        else
            mFacebookConnectItem.setTitle(getString(R.string.fb_connect_button));

        // Locate {@link MenuItem} with {@link ShareActionProvider}.
        mShareActionProvider =
                (ShareActionProvider) menu.findItem(R.id.menu_item_share).getActionProvider();
        mShareActionProvider.setOnShareTargetSelectedListener(new OnShareTargetSelectedListener() {
            @Override
            public boolean onShareTargetSelected(ShareActionProvider source, Intent intent) {
                FlurryAgent.logEvent("Share");
                EasyTracker.getTracker().sendEvent("UI", "Click", "Share", null);
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
                mMenuDrawer.toggleMenu();

                FlurryAgent.logEvent("Up");
                EasyTracker.getTracker().sendEvent("UI", "Click", "Up", null);
                return true;

            case R.id.menu_like:
                if (!TextUtils.isEmpty(mCurrentVideoId)) {
                    if (!mIsLikeItemSelected) {
                        updateLikeItem(true);
                        if (hasUserAndFacebookPermissionToPublishToFacebook())
                            publishVideo();
                        Utils.promoteVideo(mCurrentVideoId, mTitle.getText().toString());
                    }

                    Toast.makeText(this, getString(R.string.like_button_message),
                            Toast.LENGTH_SHORT).show();
                    FlurryAgent.logEvent("Like");
                    EasyTracker.getTracker().sendEvent("UI", "Click", "Like", null);
                }

                return true;

            case R.id.menu_dislike:
                if (!TextUtils.isEmpty(mCurrentVideoId)) {
                    Utils.demoteVideo(mCurrentVideoId);
                }

                if (mYouTubePlayer != null
                        && System.currentTimeMillis() - mLastSkipTimeMillis >= 2000 && next()) {
                    mLastSkipTimeMillis = System.currentTimeMillis();
                }

                Toast.makeText(this, getString(R.string.dislike_button_message), Toast.LENGTH_SHORT)
                        .show();
                FlurryAgent.logEvent("Disike");
                EasyTracker.getTracker().sendEvent("UI", "Click", "Dislike", null);
                return true;

            case R.id.menu_next:
                if (mYouTubePlayer != null
                        && System.currentTimeMillis() - mLastSkipTimeMillis >= 2000 && next()) {
                    mLastSkipTimeMillis = System.currentTimeMillis();

                    Toast.makeText(this, getString(R.string.next_button_message),
                            Toast.LENGTH_SHORT).show();
                    FlurryAgent.logEvent("Next");
                    EasyTracker.getTracker().sendEvent("UI", "Click", "Next", null);
                }
                return true;

            case R.id.menu_fb_connect:
                if (hasUserAndFacebookPermissionToPublishToFacebook()) {
                    Toast.makeText(this, getString(R.string.disconnect_fb_button_message),
                            Toast.LENGTH_SHORT).show();
                    mPref.edit().putBoolean("allowFacebookConnection", false).commit();
                    mFacebookConnectItem.setTitle(getString(R.string.fb_connect_button));
                } else {
                    mPref.edit().putBoolean("allowFacebookConnection", true).commit();
                    getFacebookPermission();
                }

                return true;

            case R.id.menu_feedback:
                Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                emailIntent.setType("message/rfc822");
                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
                        new String[] { "beautiful.lab.app@gmail.com" });
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                        "Feedback About AwwStream App");
                startActivity(Intent.createChooser(emailIntent, "Email To Developers"));

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void checkAndUpdateApp() {
        // Check and update the newest version of app.
        try {
            final int versionCode =
                    getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            final ParseObject newestVersionCode = new ParseQuery("Newest_Version_Code").getFirst();

            if (newestVersionCode.getInt("value") < versionCode) {
                newestVersionCode.put("value", versionCode);
                newestVersionCode.saveInBackground();
            }

            if (newestVersionCode.getInt("value") > versionCode) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.update_app_messasge))
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // Redirect user to app store.
                                final Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(getString(R.string.app_store_url)));
                                startActivity(intent);
                            }
                        }).setNegativeButton(R.string.no, null);
                builder.show();
            }
        } catch (Exception e) {
            // Do nothing.
        }
    }

    /**
     * Update the like item.
     */
    protected void updateLikeItem(boolean itemSelected) {
        mIsLikeItemSelected = itemSelected;
        if (mIsLikeItemSelected)
            mLikeItem.setIcon(getResources().getDrawable(R.drawable.selected_ic_action_like));
        else
            mLikeItem.setIcon(getResources().getDrawable(R.drawable.ic_action_like));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RECOVERY_DIALOG_REQUEST) {
            // Retry initialization if user performed a recovery action and there is network
            // available.
            ((YouTubePlayerSupportFragment) getSupportFragmentManager().findFragmentById(
                    R.id.youtube_fragment)).initialize(DeveloperKey.DEVELOPER_KEY, this);
        } else {
            Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {
        if (mMenuDrawer.isMenuVisible()) {
            mMenuDrawer.closeMenu();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0 && !mAdShown) {
            fullScreenAd.show();
            fullScreenAd = revmob.createFullscreen(this, null);
            mShouldQuitAutomatically = true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStop() {
        super.onStop();

        FlurryAgent.onEndSession(this);
        EasyTracker.getInstance().activityStop(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mShouldQuitAutomatically) {
            finish();
            return;
        }

        if (mYouTubePlayer != null) {
            mYouTubePlayer.loadVideo(mCurrentVideoId, mYouTubePlayer.getCurrentTimeMillis());
        }
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
        // Enter low profile mode.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            findViewById(R.id.content_frame).setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
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

    protected void markVideoAsWatched(String videoId) {
        if (TextUtils.isEmpty(videoId)) {
            return;
        }

        final ParseObject userVideo = new ParseObject("Watched");
        userVideo.put("username", mPref.getString("username", null));
        userVideo.put("videoId", videoId);
        userVideo.saveInBackground();
    }

    protected void updateShareAction(String videoId) {
        final String link = getShareActionLink(videoId);
        if (mShareActionProvider != null && !TextUtils.isEmpty(link)) {
            final Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, link);
            mShareActionProvider.setShareIntent(intent);
        }
    }

    private void publishVideo() {
        Session session = Session.getActiveSession();

        final Bundle params = new Bundle();
        params.putString("name", mTitle.getText().toString());
        params.putString("caption", "Download AwwStream Android app");
        params.putString("description", "on Google Play Store to discover more funny videos.");
        params.putString("link", getShareActionLink(mCurrentVideoId));
        params.putString(
                "actions",
                "{\"name\": \"Download App\", \"link\": \"https://play.google.com/store/apps/details?id=com.awwstream.android\"}");

        final Request.Callback callback = new Request.Callback() {
            public void onCompleted(Response response) {
                final FacebookRequestError error = response.getError();
                if (error != null) {
                    Toast.makeText(YouTubeActivity.this, error.getErrorMessage(),
                            Toast.LENGTH_SHORT).show();
                }
            }
        };

        new RequestAsyncTask(new Request(session, "me/feed", params, HttpMethod.POST, callback))
                .execute();

    }

    protected boolean hasUserAndFacebookPermissionToPublishToFacebook() {
        Session session = Session.getActiveSession();

        if (session != null && session.isOpened()
                && session.getPermissions().contains("publish_actions")
                && mPref.contains("allowFacebookConnection")
                && mPref.getBoolean("allowFacebookConnection", true)) {
            return true;
        }

        return false;
    }

    protected void getFacebookPermission() {
        final Session session = Session.getActiveSession();

        if (session == null || !session.isOpened()) {
            Session.openActiveSession(this, true, new StatusCallback() {
                @Override
                public void call(Session session, SessionState state, Exception exception) {
                    if (session != null && session.isOpened()) {
                        getFacebookPermission();
                    }
                    return;
                }
            });
            return;
        }

        // Check for publish permissions.
        if (!session.getPermissions().contains("publish_actions")) {
            final NewPermissionsRequest newPermissionsRequest =
                    new NewPermissionsRequest(this, Arrays.asList("publish_actions"));
            session.requestNewPublishPermissions(newPermissionsRequest);
            return;
        }

        // Notify that user has been connected to Facebook.
        Toast.makeText(this, getString(R.string.connect_fb_button_message), Toast.LENGTH_SHORT)
                .show();
        mFacebookConnectItem.setTitle(getString(R.string.fb_disconnect_button));

        return;
    }

    protected abstract void updateTitle(String videoId);

    protected abstract String getShareActionLink(String videoId);

    protected abstract boolean next();
}
