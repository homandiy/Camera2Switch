package com.huang.homan.camera2.Model;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.huang.homan.camera2.Presenter.CameraFragmentPresenter;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Looper.getMainLooper;

public class CameraUtil {

    /* Log tag and shortcut */
    final static String TAG = "MYLOG CameraUtil";
    public static void ltag(String message) { Log.i(TAG, message); }

    // Variables
    private Handler childHandler, mainHandler;
    private String mCameraID; // 0: rear camera; 1: front camera
    private ImageReader mImageReader;
    private CameraCaptureSession mCameraCaptureSession;
    private CameraDevice mCameraDevice;

    private Context context;
    private boolean saveToDisk = false;

    private SurfaceHolder mSurfaceHolder;
    public void startCamera(SurfaceHolder mSurfaceHolder) {
        this.mSurfaceHolder = mSurfaceHolder;
        mSurfaceHolder.setKeepScreenOn(true);
        // mSurfaceView callback: get data
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                // Initial Camera2
                initCamera2();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) { // SurfaceView removal
                // Recycle Camera
                if (null != mCameraDevice) {
                    mCameraDevice.close();
                    mCameraDevice = null;
                }
            }
        });
    }

    // Constructor
    private Album album;
    private CameraFragmentPresenter presenter;
    public CameraUtil(Album album, CameraFragmentPresenter presenter) {
        this.album = album;
        this.presenter = presenter;
    }

    /**
     * Initial Camera2
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initCamera2() {
        this.context = presenter.getContext();

        HandlerThread handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        childHandler = new Handler(handlerThread.getLooper());
        // UI thread
        mainHandler = new Handler(getMainLooper());

        mCameraID = "" + CameraCharacteristics.LENS_FACING_FRONT; // Read camera
        mImageReader = ImageReader.newInstance(1080, 1920, ImageFormat.JPEG, 1);
        // Process temporary photo data
        mImageReader.setOnImageAvailableListener(reader -> {
            // Get photo data
            Image image = reader.acquireNextImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            ltag("saveToDisk: " + saveToDisk);

            if (saveToDisk) {
                String captureTime = String.valueOf(System.currentTimeMillis());
                String pictureName = "SV_" + captureTime + ".jpg";
                File file = new File(album.getAlbumPath(), pictureName);
                try {
                    // Local storage
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    fileOutputStream.write(data);
                    fileOutputStream.close();
                    ltag("Picture created: " + pictureName);

                    // Reset
                    saveToDisk = false;

                    // Update Album
                    updateGallery(file);

                    // Show picture
                    showImage(data);

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    ltag("saveToDisk after takePhoto(): " + saveToDisk);
                    image.close();
                }
            } else {
                // Show picture
                showImage(data);
            }
        }, mainHandler);

        openCamera2View(presenter.getPermissions());
    }

    private void showImage(byte[] data) {
        final Bitmap bitmap = BitmapFactory
                .decodeByteArray(data, 0, data.length);
        if (bitmap != null) {
            presenter.setImage(bitmap);
        }
    }

    /**
     * Add the image and image album into gallery
     */
    private void updateGallery(File file) {
        Uri contentUri = Uri.fromFile(file);
        //notify gallery for new image
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri);
        context.sendBroadcast(mediaScanIntent);
    }

    /**
     * Initialize Camera
     */
    private CameraDevice.StateCallback stateCallback =
            new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) { // Open camera
            mCameraDevice = camera;
            // Preview
            takePreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) { // Turn off camera
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            presenter.showMsg(10, "Camera hardware failure.");
            ltag("Camera hardware failure.");
        }
    };

    private void openCamera2View(RxPermissions rxPermissions) {
        // get manager
        CameraManager mCameraManager =
                (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(context, CAMERA) !=
                    PackageManager.PERMISSION_GRANTED) {
                // Get WRITE_EXTERNAL_STORAGE
                rxPermissions.ensureEach(WRITE_EXTERNAL_STORAGE);
            } else {
                // Open camera
                mCameraManager.openCamera(mCameraID, stateCallback, mainHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }


    /**
     * Begin to preview
     */
    private CaptureRequest.Builder previewRequestBuilder;
    private CameraCaptureSession.StateCallback previewStateCallback =
            new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (null == mCameraDevice) return;
                    // Begin to preview
                    mCameraCaptureSession = cameraCaptureSession;
                    try {
                        // Camera2 functions
                        // Turn on Auto Focus
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        // Turn on Flash
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                        // Show up
                        CaptureRequest previewRequest = previewRequestBuilder.build();
                        mCameraCaptureSession.setRepeatingRequest(previewRequest, null, childHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    presenter.showMsg(10, "Capture Failure.");
                    ltag("Capture Failure.");
                }
            };

    private void takePreview() {
        try {
            // CaptureRequest.Builder
            previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // surface of SurfaceView will be the object of CaptureRequest.Builder
            previewRequestBuilder.addTarget(mSurfaceHolder.getSurface());
            // Create CameraCaptureSession to take care of preview and photo shooting.
            mCameraDevice.createCaptureSession(
                    Arrays.asList(mSurfaceHolder.getSurface(),
                            mImageReader.getSurface()),
                    previewStateCallback,
                    childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Camera capture
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    // Vertical Screen
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    public void takePhoto(int rotation) {
        ltag("Take Photo");

        if (mCameraDevice == null) {
            presenter.showMsg(5, "Camera is not ready or malfunction.");
            ltag("Camera is not ready or malfunction.");
            return;
        }

        saveToDisk = true;

        // Create CaptureRequest.Builder
        final CaptureRequest.Builder captureRequestBuilder;
        try {
            captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            // Surface of imageReader will be the object of CaptureRequest.Builder
            captureRequestBuilder.addTarget(mImageReader.getSurface());
            // Auto Focus
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // Auto Flash
            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // Phone Camera direction
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));
            // Get image data
            CaptureRequest mCaptureRequest = captureRequestBuilder.build();
            mCameraCaptureSession.capture(mCaptureRequest, null, childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
}
