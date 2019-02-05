package com.huang.homan.camera2.Presenter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.media.ImageReader;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.huang.homan.camera2.Model.Album;
import com.huang.homan.camera2.Model.CameraUtil;
import com.huang.homan.camera2.Model.SoundManager;
import com.huang.homan.camera2.MvpHelper.BaseActivityVP;
import com.huang.homan.camera2.MvpHelper.CameraFragmentVP;
import com.huang.homan.camera2.R;
import com.huang.homan.camera2.View.Activity.CameraActivity;
import com.huang.homan.camera2.View.Fragment.CameraFragment;
import com.huang.homan.camera2.View.common.BaseFragment;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class CameraFragmentPresenter
        implements BaseActivityVP.Presenter,
                   CameraFragmentVP.Presenter {

    /* Log tag and shortcut */
    final static String TAG = "MYLOG CameraFgPrt";
    public static void ltag(String message) { Log.i(TAG, message); }

    /* Toast shortcut */
    public static void msg(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    // Variables
    private CameraActivity cameraActivity;
    private Context fragmentContext;
    private CameraUtil cameraUtil;
    private CameraFragment cameraFragment;
    SoundManager soundManager;

    public CameraFragmentPresenter(CameraActivity cameraActivity) {
        this.cameraActivity = cameraActivity;

        // Create Album
        Album album = new Album(cameraActivity.getString(R.string.album_name));

        // Link camera with album and presenter
        cameraUtil = new CameraUtil(album, this);

        // Create Camera Fragment
        cameraFragment = new CameraFragment();
        cameraFragment.setCameraFragmentPresenter(this);
        addFragment(cameraFragment);

    }

    public void capturePhoto(int rotation) {
        cameraUtil.takePhoto(rotation);
    }

    public Context getContext() {
        fragmentContext = cameraFragment.getContext();
        if (fragmentContext == null) { //check null
            ltag("CameraFragment has not created the context.");
        }
        return fragmentContext;
    }

    public void setImage(Bitmap bitmap) {
        cameraFragment.setPicture(bitmap);
    }

    @Override
    public void addFragment(BaseFragment fragment) {
        cameraActivity.setFragment(fragment);
    }

    //region implements Show Info 
    public void showMsg(int duration, String msg) {        
        timer4Info(duration, msg);
    }
    
    private Disposable timerDisposable;
    private void timer4Info(int duration, String msg) {
        cameraFragment.showInfo(msg);

        DisposableObserver<Long> timeObserver = new DisposableObserver<Long>() {
            @Override
            public void onNext(Long aLong) {
                ltag("Time is running at "+aLong);
            }

            @Override
            public void onError(Throwable e) {
                ltag("Timer error: "+e.getMessage());
            }

            @Override
            public void onComplete() {
                ltag("Count down finished.");
                cameraFragment.hideInfo();
                stopTimer();
            }
        };

        timerDisposable = Observable
                .interval(1, TimeUnit.SECONDS)
                .take(duration)
                .map(v -> { return duration - v; })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .subscribeWith(timeObserver);

        ltag("Start timer for Info.");
    }
    
    private void stopTimer() {
        if (timerDisposable != null) {
            if (!timerDisposable.isDisposed()) {
                timerDisposable.dispose();
                ltag("timerDisposable is disposed.");
            }
        }
    }
    //endregion implements Show Info

    @Override
    public void setFragment(CameraFragment cameraFragment) {
        this.cameraFragment = cameraFragment;
        soundManager = SoundManager.getInstance(getContext());
    }

    public void setupCamera(int width, int height) {
        cameraUtil.setupCamera(width, height);
    }


    public void openCamera(RxPermissions rxPermissions) {
        cameraUtil.openCamera(rxPermissions);
    }

    public SurfaceTexture getSurfaceTexture() {
        return cameraFragment.getmTextureView().getSurfaceTexture();
    }


    public void playShutter() {
        soundManager.playShutter();
    }

    public void playShutter2() {
        soundManager.playShutter2();
    }

    public void releaseSound() {
        soundManager.release();
    }
}
