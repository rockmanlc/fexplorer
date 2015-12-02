package com.rocklee.fexplorer;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;

public class FExplorerActivity extends ListActivity {
    private static final String TAG = "LC_fexplorer";
    private ArrayList<String> fileNames = null;
    private ArrayList<String> filespaths = null;
    private String MediaPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fexplorer);
        File file= Environment.getExternalStorageDirectory();
        MediaPath=file.getAbsolutePath();
        showFileDir(MediaPath);
    }

    private void showFileDir(String path){
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
        for (File f : files){
            fileNames.add(f.getName());
            filespaths.add(f.getPath());
        }
        this.setListAdapter(new MyAdapter(this, fileNames, filespaths));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        String path = filespaths.get(position);
        File file = new File(path);

        if (file.exists() && file.canRead()){
            if (file.isDirectory()){
                showFileDir(path);
            }
            else{
                fileHandle(file);
            }
        }
        else {
            Resources res = getResources();
            new AlertDialog.Builder(this).setTitle("Message")
                    .setMessage(res.getString(R.string.no_permission))
                    .setPositiveButton("OK",new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).show();
        }
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

        String type = getMIMEType(file);
        if (type.equals("gif/*")) {
            Log.d(TAG, type + " and " + file.getAbsolutePath());
            Intent intentGif = new Intent(FExplorerActivity.this, PhotoPlayerActivity.class);
            intentGif.putExtra("gif_path", file.getAbsolutePath());
            startActivity(intentGif);
        } else if (type.equals("video/*")) {
            Log.d(TAG, type + " and " + file.getAbsolutePath());
            Intent intentVideo = new Intent(FExplorerActivity.this, VideoPlayerActivity.class);
            //Intent intentVideo = new Intent(FExplorerActivity.this, DecodeActivity.class);
            intentVideo.putExtra("video_path", file.getAbsolutePath());
            startActivity(intentVideo);
        } else {
            intent.setDataAndType(Uri.fromFile(file), type);
            startActivity(intent);
        }
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
