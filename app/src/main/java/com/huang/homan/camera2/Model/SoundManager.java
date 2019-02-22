package com.huang.homan.camera2.Model;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import com.huang.homan.camera2.R;

import static android.content.Context.AUDIO_SERVICE;

public class SoundManager {
    /* Log tag and shortcut */
    final static String TAG = "MYLOG SoundMG";
    public static void ltag(String message) { Log.i(TAG, message); }

    private float actualVolume;
    private float maxVolume;
    private float volume;
    private AudioManager audioManager;

    // Sound players
    MediaPlayer shutterPlay;
    MediaPlayer shutter2Play;
    MediaPlayer switchPlay;

    private static SoundManager instance;

    private Context context;
    private SoundManager(Context context) {
        this.context = context;
        // AudioManager audio settings for adjusting the volume
        audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
        actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        volume = maxVolume;

        shutterPlay = MediaPlayer.create(context, R.raw.shutter);
        shutterPlay.setVolume(maxVolume, maxVolume);
        shutter2Play = MediaPlayer.create(context, R.raw.shutter2);
        shutter2Play.setVolume(maxVolume, maxVolume);
        switchPlay = MediaPlayer.create(context, R.raw.switch_camera);
        switchPlay.setVolume(maxVolume, maxVolume);
    }

    public static SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context);
        }
        return instance;
    }

    public void playSound(MediaPlayer mPlayer) {
        ltag("Playing sound.");
        mPlayer.start();
    }

    public void playShutter() {
        ltag("Playing shutter sound.");
        playSound(shutterPlay);
    }

    public void playShutter2() {
        playSound(shutter2Play);
    }

    public void playSwitch() {
        playSound(switchPlay);
    }

    public void release() {
        if (shutterPlay != null) {
            shutterPlay.stop();
            shutterPlay.release();
            shutterPlay = null;
        }
        if (shutter2Play != null) {
            shutter2Play.stop();
            shutter2Play.release();
            shutter2Play = null;
        }
        instance = null;
    }
}
