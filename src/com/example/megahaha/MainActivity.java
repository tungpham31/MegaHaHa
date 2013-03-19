package com.example.megahaha;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {
	public static final String PREFS_NAME = "MyPrefsFile";
	private SharedPreferences mPref;
	private SharedPreferences.Editor mPrefEditor;
	private boolean mFirstTimeStart;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		/* Does not support this function in this version
		mPref = getSharedPreferences(PREFS_NAME, 0);
		mPrefEditor = mPref.edit();
		mFirstTimeStart = mPref.getBoolean("mFirstTimeStart", true);
		if (!mFirstTimeStart){
			startStreaming();
		}
		else{
			mFirstTimeStart = false;
			mPrefEditor.putBoolean("mFirstTimeStart", mFirstTimeStart);
			mPrefEditor.commit();
		}*/
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		startStreaming();
	}
	
	public void startStreaming(View view){
		startStreaming();
	}
	public void startStreaming(){
		finish();
		Intent intent = new Intent(this, StreamActivity.class);
		startActivity(intent);
	}
}
