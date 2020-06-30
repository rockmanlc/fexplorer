package com.rocklee.fexplorer.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.rocklee.fexplorer.GifView;
import com.rocklee.fexplorer.R;

public class PhotoPlayerActivity extends Activity {
    private static final String TAG = "LC_PhotoPlayerActivity";
    private String filePath = "";
    protected GifView mGifView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_player);
        Intent intent = getIntent();
        filePath = intent.getStringExtra("gif_path");
        mGifView = (GifView) findViewById(R.id.gifView);
        decodeGif(filePath);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        mGifView.setStop();
        super.onStop();
    }

    private void decodeGif(final String url) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isDecodeSuccess = mGifView.setSrc(url, PhotoPlayerActivity.this);
                if (isDecodeSuccess) {
                    if (mGifView.getFrameCount() > 1) {
                        mGifView.setStart();
                    }
                }
            }
        }).start();
    }
}
