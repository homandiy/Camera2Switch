package com.huang.homan.camera2.Model;

import android.os.Environment;
import android.util.Log;

import java.io.File;

public class Album {
    /* Log tag and shortcut */
    final static String TAG = "MYLOG Album";
    public static void ltag(String message) { Log.i(TAG, message); }

    private String albumName;

    private String albumPath;
    public String getAlbumPath() {
        return albumPath;
    }

    public Album(String albumName) {
        this.albumName = albumName;
        createAlbum();
    }

    public void createAlbum() {
        // Check for SD card storage
        String status = Environment.getExternalStorageState();
        if (!status.equals(Environment.MEDIA_MOUNTED)) {
            ltag("Use Internal Storage: SD card is not available.");
        } else {
            ltag("Use External Storage.");
        }

        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), albumName);
        if (!file.mkdirs()) {
            albumPath = file.getAbsolutePath();
            ltag("Album existed: " + albumPath);
        } else {
            albumPath = file.getAbsolutePath();
            ltag("Album created: " + albumPath);
        }
    }
}
