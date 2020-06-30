package com.rocklee.fexplorer.utils;

import android.net.Uri;

import java.io.Serializable;

public class VideoContainer implements Serializable {
    public final String uri;
    public final String name;
    public final int duration;
    public final int size;
    public final int id;

    public VideoContainer(String uri, String name, int id, int duration, int size) {
        this.uri = uri;
        this.id = id;
        this.name = name;
        this.duration = duration;
        this.size = size;
    }
}
