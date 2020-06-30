package com.rocklee.fexplorer.utils;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class QueryMedia {
    private final static String TAG = "LC_QueryMedia";
    List<VideoContainer> mediaList = new ArrayList<>();

    String[] projection = new String[] {
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE
    };
    String selection = MediaStore.Video.Media.DURATION +
            " >= ?";
    String[] selectionArgs = new String[] {
            String.valueOf(TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS))};
    String sortOrder = MediaStore.Video.Media.DATE_MODIFIED + " ASC";

    public QueryMedia(List<VideoContainer> list) {
        mediaList = list;
    }

    public List<VideoContainer> getMediaList(Context context) {
        Log.i(TAG, "getMediaList entry");

        Cursor cursor = context.getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder);

        // Cache column indices.
        int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
        int nameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
        int durationColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
        int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);

        while (cursor.moveToNext()) {
            // Get values of columns for a given video.
            int id = cursor.getInt(idColumn);
            String name = cursor.getString(nameColumn);
            int duration = cursor.getInt(durationColumn);
            int size = cursor.getInt(sizeColumn);

            Uri contentUri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id);

            // Stores column values and the contentUri in a local object
            // that represents the media file.
            mediaList.add(new VideoContainer(contentUri.toString(), name, id, duration, size));
            //Log.i(TAG, "id is " + id + ", name is " + name + ", uri is " + contentUri);
        }
        cursor.close();
        return mediaList;
    }
}
