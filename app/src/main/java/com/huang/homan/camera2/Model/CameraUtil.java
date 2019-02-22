package com.huang.homan.camera2.Model;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.huang.homan.camera2.Presenter.CameraFragmentPresenter;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Looper.getMainLooper;

public class CameraUtil {

    /* Log tag and shortcut */
    final static String TAG = "MYLOG CameraUtil";
    public static void ltag(String message) { Log.i(TAG, message); }

    // Variables
    // Thread and Handlers
    private Handler childHandler, mainHandler;
    private HandlerThread mBackgroundThread;

    // Permission
    private boolean permissionGranted = false;
    public void setPermission(boolean permissionGranted) {
        this.permissionGranted = permissionGranted;
    }

    private String mCameraId; // 0: rear camera; 1: front camera
    private ImageReader mImageReader;
    private CameraCaptureSession mCameraCaptureSession;
    private CameraDevice mCameraDevice;
    private CameraManager mCameraManager;

    private Context context;
    private boolean saveToDisk = false;

    // Constructor
    private Album album;
    private CameraFragmentPresenter presenter;
    public CameraUtil(Album album, CameraFragmentPresenter presenter) {
        this.album = album;
        this.presenter = presenter;
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


    @SuppressLint("MissingPermission")
    public void openCamera() {
        try {
            if (permissionGranted) {
                mCameraManager.openCamera(mCameraId, stateCallback, mainHandler);
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
        SurfaceTexture mSurfaceTexture = presenter.getSurfaceTexture();
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(mSurfaceTexture);

        try {
            // CaptureRequest.Builder
            previewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // surface of SurfaceView will be the object of CaptureRequest.Builder
            previewRequestBuilder.addTarget(previewSurface);
            // Create CameraCaptureSession to take care of preview and photo shooting.
            mCameraDevice.createCaptureSession(
                    Arrays.asList(previewSurface, mImageReader.getSurface()),
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


    private Size mPreviewSize;
    private Size mCaptureSize;

    private static final String CAMERA_FONT = "0";
    private static final String CAMERA_BACK = "1";

    public void loadThread() {
        if (mBackgroundThread == null) {
            mBackgroundThread = new HandlerThread("Camera2");
            mBackgroundThread.start();
            childHandler = new Handler(mBackgroundThread.getLooper());
            // UI thread
            mainHandler = new Handler(getMainLooper());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void setupCamera(int width, int height) {
        ltag("Setup camera: x = "+width+"    , y = "+height);
        try {
            assert mCameraId != null;
            CameraCharacteristics characteristics =
                    mCameraManager.getCameraCharacteristics(mCameraId);
            // StreamConfigurationMap: manage format and size
            StreamConfigurationMap map = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            // set preview size by TextureView size
            mPreviewSize = getOptimalSize(
                    map.getOutputSizes(SurfaceTexture.class),
                    width,
                    height);

            // get max size of camera
            mCaptureSize = Collections.max(Arrays.asList(
                    map.getOutputSizes(ImageFormat.JPEG)), (lhs, rhs) ->
                    Long.signum(lhs.getWidth() * lhs.getHeight() -
                            rhs.getHeight() * rhs.getWidth()));
            // ImageReader: take picture and preview
            setupImageReader();
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void switchCamera() {
        if (TextUtils.equals(mCameraId, CAMERA_FONT)) {
            mCameraId = CAMERA_BACK;
        } else {
            mCameraId = CAMERA_FONT;
        }
        closeCamera();
        reopenCamera();
    }

    public void closeCamera() {
        if (null != mCameraCaptureSession) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }

        if (null != mCameraDevice) {
            mCameraDevice.close();
            mCameraDevice = null;
        }

        if (null != mImageReader) {
            mImageReader.close();
            mImageReader = null;
        }
    }


    /* check camera: if we have two cameras, return the front
              else return anything for one camera
         */
    private int cameraQuanity = 0;
    public int getCameraQuanity() { return cameraQuanity; }
    private int textureHeight;
    private int textureWidth;
    public String getCameraId(int width, int height) {
        context = presenter.getContext();
        textureWidth = width;
        textureHeight = height;

        mCameraManager = (CameraManager)
                context.getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraList = mCameraManager.getCameraIdList();
            cameraQuanity = cameraList.length;
            for (int i = 0; i < cameraQuanity; i++) {
                String cameraId = cameraList[i];
                if (TextUtils.equals(cameraId, CAMERA_FONT)) {
                    mCameraId = cameraId;
                    break;
                } else if (TextUtils.equals(cameraId, CAMERA_BACK)) {
                    mCameraId = cameraId;
                    break;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void setupImageReader() {
        mImageReader = ImageReader.newInstance(mCaptureSize.getWidth(), mCaptureSize.getHeight(),
                ImageFormat.JPEG, 1);
        // Process temporary photo data
        mImageReader.setOnImageAvailableListener(reader -> {
            // Get photo data
            Image image = reader.acquireNextImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);

            ltag("saveToDisk: " + saveToDisk);

            if (saveToDisk) {
                saveImage(data, image);
            } else {
                // Show picture
                showImage(data);
            }
        }, mainHandler);
    }

    private void saveImage(byte[] data, Image image) {
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
    }

    private void showImage(byte[] data) {
        final Bitmap bitmap = BitmapFactory
                .decodeByteArray(data, 0, data.length);
        if (bitmap != null) {
            presenter.setImage(bitmap);
        }
    }

    // sizeMap: get nearest size of width and height
    private Size getOptimalSize(Size[] sizeMap, int width, int height) {
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList, (lhs, rhs) ->
                    Long.signum(lhs.getWidth() * lhs.getHeight() -
                            rhs.getWidth() * rhs.getHeight()));
        }
        return sizeMap[0];
    }

    public void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mainHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void reopenCamera() {
        ltag("Reopen Camera");
        setupCamera(textureWidth, textureHeight);
        if (presenter.checkTextureViewAvailable()) {
            openCamera();
        } else {
            presenter.setTextureListener();
        }
    }
}
