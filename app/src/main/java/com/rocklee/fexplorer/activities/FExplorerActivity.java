package com.rocklee.fexplorer.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.text.TextUtils;

import com.rocklee.fexplorer.utils.*;
import com.rocklee.fexplorer.R;

import java.io.File;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

public class FExplorerActivity extends ListActivity {
    private static final String TAG = "LC_fexplorer";
    private static boolean DEBUG = true;
    private ArrayList<String> fileNames = null;
    private ArrayList<String> filespaths = null;
    private ArrayList<String> filesId= null;
    private String MediaPath;
    private ContentResolver mContentResolver;
    private QueryMedia mQueryMedia;
    private List<VideoContainer> videoList = new ArrayList<>();
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle state) {
        if (RequestPermissionsActivity.startPermissionActivity(this)) {
            finish();
        }
        if (DEBUG) {
            Log.d(TAG, "FExplorerActivity.onCreate");
        }
        mQueryMedia = new QueryMedia(videoList);
        super.onCreate(state);
    }

    protected void doInBackground(Void... params) {
//        mContentResolver = this.getContentResolver();
//        fileNames = new ArrayList<String>();
//        filespaths = new ArrayList<String>();
//        filesId = new ArrayList<String>();
//
//        String[] mediaColumns = { MediaStore.Video.Media._ID, MediaStore.Video.Media.DATA,
//                MediaStore.Video.Media.TITLE, MediaStore.Video.Media.MIME_TYPE,
//                MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.SIZE,
//                MediaStore.Video.Media.DATE_ADDED, MediaStore.Video.Media.DURATION,
//                MediaStore.Video.Media.WIDTH, MediaStore.Video.Media.HEIGHT };
//
//        Cursor mCursor = mContentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mediaColumns,
//                null, null, MediaStore.Video.Media.DATE_ADDED);
//
//        if (mCursor == null) {
//            return;
//        }
//
//        int ixData = mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
//        int ixMime = mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE);
//        Log.i(TAG, "ixData is " + ixData + ", ixMime is " + ixMime);
//
//        int ixId = mCursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
//        int ixSize = mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
//        int ixTitle = mCursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
//        Log.i(TAG, "ixId is " + ixId + ", ixSize is " + ixSize + ", ixTitle is " + ixTitle);
//
//        mCursor.moveToLast();
//
//        while (mCursor.moveToPrevious()) {
///*            if (addVideo(mCursor) == 0) {
//                continue;
//            } else if (addVideo(mCursor) == 1) {
//                break;
//            }*/
//            int videoId = mCursor.getInt(mCursor.getColumnIndex(MediaStore.Video.Media._ID));
//            filesId.add(String.valueOf(videoId));
//            String path;
//            if(Build.VERSION.SDK_INT == Build.VERSION_CODES.P){
//                path =MediaStore.Video.Media
//                        .EXTERNAL_CONTENT_URI
//                        .buildUpon()
//                        .appendPath(String.valueOf(videoId)).build().toString();
//            }else{
//                path = mCursor.getString(mCursor.getColumnIndex(MediaStore.Video.Media.DATA));
//            }
//            filespaths.add(path);
//            long duration = mCursor.getLong(mCursor.getColumnIndex(MediaStore.Video.Media.DURATION));
//            long size = mCursor.getLong(mCursor.getColumnIndex(MediaStore.Video.Media.SIZE)) / 1024; //单位kb
//            if (size < 0) {
//                Log.e("dml", "this video size < 0 " + path);
//                size = new File(path).length() / 1024;
//            }
//            String displayName = mCursor.getString(mCursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
//            fileNames.add(displayName);
//
//            int timeIndex = mCursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED);
//            Long date = mCursor.getLong(timeIndex) *1000;
//            Log.i(TAG, "videoId is " + videoId + ", path is " + path + ", displayName is " + displayName);
//        }
        if (videoList.size() > 0) {
            videoList.clear();
        }
        videoList = mQueryMedia.getMediaList(this);
        for(int i = 0; i < videoList.size(); i++) {
            Log.i(TAG, "videoList id is " + videoList.get(i).id + ", name is " + videoList.get(i).name);
        }
        this.setListAdapter(new MyAdapter(this, videoList));

        //mCursor.close();
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        Log.d(TAG, "onContentChanged");
    }

    public void onStart() {
        doInBackground();
        super.onStart();
        Log.d(TAG, "onStart");
    }

    public void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    public void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Log.d(TAG, "onPostCreate");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    public void onPostResume() {
        super.onPostResume();
        Log.d(TAG, "onPostResume");
    }

    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged");
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState");
    }

    public void onRestoreInstanceState(Bundle outState) {
        super.onRestoreInstanceState(outState);
        Log.d(TAG, "onRestoreInstanceState");
    }


    private void showFileDir(String path){
        if (TextUtils.isEmpty(path)) {
            Log.w(TAG, "Empty file path, bailing out");
            return;
        }
        fileNames = new ArrayList<String>();
        filespaths = new ArrayList<String>();
        File file = new File(path);
        File[] files = file.listFiles();

        if (!MediaPath.equals(path)) {
            fileNames.add("@1");
            filespaths.add(MediaPath);

            fileNames.add("@2");
            filespaths.add(file.getParent());
        }
        if(files != null) {
            for (File f : files) {
                fileNames.add(f.getName());
                filespaths.add(f.getPath());
            }
            this.setListAdapter(new MyAdapter(this, fileNames, filespaths));
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Log.i(TAG, "onListItemClick");
        //String path = MediaStore.Video.Media.EXTERNAL_CONTENT_URI.buildUpon().appendPath(filesId.get(position)).build().toString();
        //String path = filespaths.get(position);
        VideoContainer videoContainer = videoList.get(position);
        String videoUri = videoContainer.uri;
        Log.i(TAG, "videoUri is " + videoUri);
        //File file = new File(path);
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(android.content.Intent.ACTION_VIEW);

        Intent intentVideo = new Intent(FExplorerActivity.this, VideoPlayerActivity.class);
        intentVideo.putExtra("video_uri", videoContainer);
        startActivity(intentVideo);

        //if (file.exists() && file.canRead()){
//        if (true) {
//            if (file.isDirectory()){
//                showFileDir(path);
//            }
//            else{
//                Log.i(TAG, "open file");
//                fileHandle(file);
//            }
//        }
//        else {
//            Resources res = getResources();
//            new AlertDialog.Builder(this).setTitle("Message")
//                    .setMessage(res.getString(R.string.no_permission))
//                    .setPositiveButton("OK",new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//
//                        }
//                    }).show();
//        }
        super.onListItemClick(l, v, position, id);
    }

    private void fileHandle(final File file) {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0){
                    openFile(file);
                }
            }
        };

        String[] menu = {"打开文件","重命名","删除文件"};
        new AlertDialog.Builder(FExplorerActivity.this)
                .setTitle("请选择要进行的操作!")
                .setItems(menu, listener)
                .setPositiveButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }

    private void openFile(File file){
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(android.content.Intent.ACTION_VIEW);

        Intent intentVideo = new Intent(FExplorerActivity.this, VideoPlayerActivity.class);
        intentVideo.putExtra("video_path", file.getAbsolutePath());
        startActivity(intentVideo);

//        String type = getMIMEType(file);
//        if (type.equals("gif/*")) {
//            Log.d(TAG, type + " and " + file.getAbsolutePath());
//            Intent intentGif = new Intent(FExplorerActivity.this, PhotoPlayerActivity.class);
//            intentGif.putExtra("gif_path", file.getAbsolutePath());
//            startActivity(intentGif);
//        } else if (type.equals("video/*")) {
//            Log.d(TAG, type + " and " + file.getAbsolutePath());
//            Intent intentVideo = new Intent(FExplorerActivity.this, VideoPlayerActivity.class);
//            //Intent intentVideo = new Intent(FExplorerActivity.this, DecodeActivity.class);
//            intentVideo.putExtra("video_path", file.getAbsolutePath());
//            startActivity(intentVideo);
//        } else {
//            intent.setDataAndType(Uri.fromFile(file), type);
//            startActivity(intent);
//        }
    }

    private String getMIMEType(File file) {
        String type = "";
        String name = file.getName();
        String end = name.substring(name.lastIndexOf(".") + 1, name.length()).toLowerCase();
        if (end.equals("m4a") || end.equals("mp3") || end.equals("wav")){
            type = "audio";
        }
        else if(end.equalsIgnoreCase("mp4") ||
                end.equalsIgnoreCase("3gp") || end.equalsIgnoreCase("mov")) {
            type = "video";
        }
        else if (end.equals("jpg") || end.equals("png") || end.equals("jpeg") || end.equals("bmp")){
            type = "image";
        }
        else if (end.equalsIgnoreCase("gif")) {
            type = "gif";
        }
        else {
            type = "*";
        }
        type += "/*";
        return type;
    }
}
