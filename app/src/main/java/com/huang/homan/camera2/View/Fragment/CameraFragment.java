package com.huang.homan.camera2.View.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.huang.homan.camera2.MvpHelper.CameraFragmentVP;
import com.huang.homan.camera2.Presenter.CameraFragmentPresenter;
import com.huang.homan.camera2.R;
import com.huang.homan.camera2.View.common.AutoFitTextureView;
import com.huang.homan.camera2.View.common.BaseFragment;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.annotations.NonNull;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.content.Context.SENSOR_SERVICE;
import static android.hardware.SensorManager.SENSOR_DELAY_UI;
import static com.huang.homan.camera2.R2.string.pitch;
import static java.lang.String.format;
import static java.util.Calendar.SECOND;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CameraFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CameraFragment extends BaseFragment
        implements CameraFragmentVP.View, SensorEventListener {

    /* Log tag and shortcut */
    final static String TAG = "MYLOG CameraFrag";

    public static void ltag(String message) {
        Log.i(TAG, message);
    }

    /* Toast shortcut */
    public static void msg(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    // user permission
    private boolean permissionGranted = false;

    // UI Variables
    //region implements TextureView related
    @BindView(R.id.mTextureView)
    AutoFitTextureView mTextureView;

    public AutoFitTextureView getTextureView() {
        if (mTextureView == null) { //check null
            ltag("View object has not created.");
        }
        return mTextureView;
    }

    /**
     * If quanity of camera < 2, hide switch button
     */
    private void checkCameraQuanity() {
        if (presenter.getCameraQuanity() < 2) {
            switchIV.setVisibility(View.GONE);
        }
    }

    private TextureView.SurfaceTextureListener mTextureListener =
            new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            presenter.getDefaultCamera(width, height); //get default camera ID
            checkCameraQuanity();
            presenter.setupThread();
            presenter.setupCamera(width, height);
            presenter.openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };
    //endregion implements TextureView Related


    public AutoFitTextureView getmTextureView() {
        if (mTextureView == null) { //check null
            ltag("View object has not created.");
        }
        return mTextureView;
    }

    @BindView(R.id.infoTV)
    TextView infoTV;

    @BindView(R.id.cameraCL)
    ConstraintLayout cameraCL;

    @BindView(R.id.iv_show)
    ImageView iv_show;

    public void setPicture(Bitmap bitmap) {
        iv_show.setImageBitmap(bitmap);
    }

    //region implements Control Panel variables
    private int offsetX;
    private int offsetY;

    @BindView(R.id.crossDotIV)
    ImageView crossDotIV;

    @BindView(R.id.controlLV)
    LinearLayout controlLV;

    // Control Panel data
    private int controlHeight;
    private int controlWidth;
    // Control Panel position
    private position cpPosition = position.BottomRight;

    // Capture button
    @BindView(R.id.captureIV)
    ImageView captureIV;

    @OnClick(R.id.captureIV)
    public void captureImageClicked() {
        ltag("Capture Button Clicked.");

        presenter.playShutter();
        //presenter.playShutter2();
        //presenter.capturePhoto(getRotation());
    }

    // Switch camera button
    @BindView(R.id.switchIV)
    ImageView switchIV;

    @OnClick(R.id.switchIV)
    public void switchCameraClicked() {
        ltag("Switch Button Clicked.");

        presenter.playSoundSwitchCamera();
        presenter.switchCamera();
    }
    //endregion implements

    // Rotation Sensor variables
    @BindView(R.id.rvInclude)
    View rvInclude;
    @BindView(R.id.rvData1)
    TextView rvData1;
    @BindView(R.id.rvData2)
    TextView rvData2;
    @BindView(R.id.rvData3)
    TextView rvData3;
    @BindView(R.id.rvData4)
    TextView rvData4;

    // Tilt sensors variables
    @BindView(R.id.tiltInclude)
    View tiltInclude;
    @BindView(R.id.azimuthData)
    TextView azimuthData;
    @BindView(R.id.pitchData)
    TextView pitchData;
    @BindView(R.id.rollData)
    TextView rollData;

    private int getRotation() {
        return getActivity().getWindowManager().getDefaultDisplay().getRotation();
    }

    Unbinder unbinder;

    // Sensor Manager
    private SensorManager mSensorManager;
    private List<Sensor> sensorList = new ArrayList<>();
    private Sensor mRotationSensor;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;

    // Variables
    private CameraFragmentPresenter presenter;
    public void setCameraFragmentPresenter(
            @NonNull CameraFragmentPresenter presenter) {
        this.presenter = presenter;
    }

    // RxJava: RxPermission
    public RxPermissions rxPermissions;

    private OnFragmentInteractionListener mListener;

    public CameraFragment() {
        // Required empty public constructor
    }

    /**
     * Link Fragment and its presenter
     */
    public static CameraFragment newInstance() {
        return new CameraFragment();
    }

    @SuppressLint("CheckResult")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // lock the rotation
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Permissions
        rxPermissions = new RxPermissions(this);
        rxPermissions.setLogging(true);
        rxPermissions
                .request(CAMERA,
                         READ_EXTERNAL_STORAGE,
                         WRITE_EXTERNAL_STORAGE)
                .subscribe(granted -> {
                    ltag("Permission: " + granted.toString());
                    if (granted) { // Always true pre-M
                        msg(getContext(), "Permissions granted!");
                        ltag("Permissions granted!");

                        setupSensors();
                        permissionGranted = true;

                    } else {
                        // Oops permission denied
                        msg(getContext(), "You don't have the permission!");
                        ltag("You don't have the permission!");
                    }
                });
    }

    private void setupSensors() {
        try {
            mSensorManager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
            mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        } catch (Exception e) {
            msg(getContext(), "Hardware compatibility issue");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ltag("onStart()");
    }

    @Override
    public void onResume() {
        super.onResume();
        ltag("onResume()");
        presenter.setupThread();
        if (cameraCL.getWidth() > 0) {
            registerSensors();
            presenter.openCamera();
            // Reset button position
            captureIV.setX((float) LayoutLocationMap.get(position.BottomRight).getX());
            captureIV.setY((float) LayoutLocationMap.get(position.BottomRight).getY());
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        presenter.closeCamera();
        presenter.stopBackgroundThread();
    }

    private void registerSensors() {
        ltag("Register Sensors");
        if (mRotationSensor != null) {
            mSensorManager.registerListener(
                    this,
                    mRotationSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (mAccelerometer != null) {
            mSensorManager.registerListener(
                    this,
                    mAccelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (mMagnetometer != null) {
            mSensorManager.registerListener(
                    this,
                    mMagnetometer,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ltag("onCreateView()");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        unbinder = ButterKnife.bind(this, view);

        if (presenter != null) presenter.setPermission(permissionGranted);

        // Get fragment measurement
        ViewTreeObserver vto = cameraCL.getViewTreeObserver();
        vto.addOnGlobalLayoutListener (
                new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                cameraCL.getViewTreeObserver()
                        .removeOnGlobalLayoutListener(this);
                int height = cameraCL.getMeasuredWidth();
                int width = cameraCL.getMeasuredHeight();

                ltag("fragment ~ h: "+height+"    w: "+width);

                controlHeight = controlLV.getHeight();
                controlWidth = controlLV.getWidth();

                ltag("capture bar ~ h: "+controlHeight+"    w: "+controlWidth);
                int location[] = new int[2];
                controlLV.getLocationOnScreen(location);

                offsetX = height - location[0] - controlWidth;
                ltag("offsetX: height("+height+") - location0("+location[0]+") " +
                        "- controlW("+controlWidth+") = "+offsetX);
                offsetY = width - location[1] - controlHeight;
                ltag("offsetY: width("+width+") - location1("+location[1]+") " +
                        "- controlH("+controlHeight+") = "+offsetY);

                //location checker
                crossDotIV.setVisibility(View.VISIBLE);
                crossDotIV.setX(location[0]-20);
                crossDotIV.setY(location[1]-20);

                createPanelLocationMap(height, width, controlHeight, controlWidth);

                registerSensors();
            }
        });

        return view;
    }


    @Override
    public void onViewCreated(@android.support.annotation.NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setTextureListener();

        presenter.setFragment(this);

        //getSensorList();
    }

    private void getSensorList() {
        sensorList = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        StringBuilder sensorText = new StringBuilder();

        for (Sensor currentSensor : sensorList ) {
            sensorText.append(currentSensor.getName()).append(
                    System.getProperty("line.separator"));
        }

        ltag("sensors: "+sensorText);
    }

    @Override
    protected int getLayout() {
        return 0;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
        presenter.releaseSound();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    public boolean getTextureViewAvailable() {
        return mTextureView.isAvailable();
    }

    public void setTextureListener() {
        mTextureView.setSurfaceTextureListener(mTextureListener);
    }

    //region implements Sensor Implementation
    //region implements Layout Location Map
    public enum position {
        TopLeft, TopRight, BottomLeft, BottomRight
    }

    private class LayoutLocation{
        private int x;
        private int y;

        public LayoutLocation(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public String toString() {
            return "x: "+x+",  y: "+y;
        }
    }

    private Map<position, LayoutLocation> LayoutLocationMap = new HashMap<>();

    /**
     * Map Control Panel Location after rotation
     * phone example: offsetX=32 offsetY=16
     * @param x screen width,               phone example: 720
     * @param y screen height,              phone example: 1280
     * @param cpx control panel width       phone example: 320
     * @param cpy control panel height      phone example: 160
     */
    private void createPanelLocationMap(int x, int y, int cpy, int cpx) {
        // set top left position
        LayoutLocationMap.put(position.TopLeft,
                new LayoutLocation(offsetX+cpx, offsetY+cpy));
        // set top right position
        LayoutLocationMap.put(position.TopRight,
                new LayoutLocation(x-offsetY-cpy, offsetX+cpx));
        // set bottom left position
        LayoutLocationMap.put(position.BottomLeft,
                new LayoutLocation(offsetX+cpy, y-offsetY-cpx));
        // set top right position
        LayoutLocationMap.put(position.BottomRight,
                new LayoutLocation(x-offsetX-cpx, y-offsetY-cpy));
    }
    //endregion implements

    /**
     * Sensors change
     */
    private float[] mAccelerometerData = new float[3];
    private float[] mMagnetometerData = new float[3];
    private float[] mVectors  = new float[9];
    @SuppressLint("DefaultLocale")
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        int sensorType = sensorEvent.sensor.getType();
        switch (sensorType) {
            case Sensor.TYPE_ACCELEROMETER:
                mAccelerometerData = sensorEvent.values.clone();
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                mMagnetometerData = sensorEvent.values.clone();
                break;

            case Sensor.TYPE_ROTATION_VECTOR:
                mVectors = sensorEvent.values.clone();
                rvData1.setText(getResources().getString(
                        R.string.float_format, mVectors[0]));
                rvData2.setText(getResources().getString(
                        R.string.float_format, mVectors[1]));
                rvData3.setText(getResources().getString(
                        R.string.float_format, mVectors[2]));
                rvData4.setText(getResources().getString(
                        R.string.float_format, mVectors[3]));
                break;

            default:
                return;
        }

        // get matrix
        float[] mOrientationValues = getMatrix(mAccelerometerData, mMagnetometerData);
        if (mOrientationValues != null) {
            // update tilt data
            float azimuth = mOrientationValues[0];
            float pitch = mOrientationValues[1];
            float roll = mOrientationValues[2];

            azimuthData.setText(getResources().getString(
                    R.string.float_format, azimuth));
            pitchData.setText(getResources().getString(
                    R.string.float_format, pitch));
            rollData.setText(getResources().getString(
                    R.string.float_format, roll));

            moveControlBar(pitch, roll, mVectors[0], mVectors[2]);
        } else {
            // update tilt data with error
            azimuthData.setText(getResources().getString(
                    R.string.error_matrix));
            pitchData.setText(getResources().getString(
                    R.string.error_matrix));
            rollData.setText(getResources().getString(
                    R.string.error_matrix));
        }
    }

    /**
     * Guideline:
     * Portrait: bottom right, 0 degree
     * Portrait upside down: top left and rotate 180
     * Landscape left upside: top right and rotate 90
     * Landscape right upside: bottom left and rotate 270
     * @param pitch up/down
     * @param roll left/right
     */
    private void moveControlBar(float pitch, float roll, float v1, float v3) {

        position newPos = getPosition(pitch, roll, v1, v3);

        if (newPos != cpPosition) {
            ltag("Old Pos: "+cpPosition.name()+"    --   New Pos: "+newPos.name());
            int location[] = new int[2];

            controlLV.getLocationOnScreen(location);
            ltag("Before:  x: "+location[0]+"  y: "+location[1]);

            // Setup the pivot point
            controlLV.setPivotX(0);
            controlLV.setPivotY(0);

            switch (newPos) {
                case TopLeft:
                    controlLV.setRotation(180);
                    break;
                case TopRight:
                    controlLV.setRotation(-90);
                    break;
                case BottomLeft:

                    controlLV.setRotation(90);
                    break;
                case BottomRight:
                    controlLV.setRotation(0);
                    break;
            }

            LayoutLocation oldBL = LayoutLocationMap.get(cpPosition);
            LayoutLocation newBL = LayoutLocationMap.get(newPos);
            ltag("Old Location: "+oldBL.toString()+".  New Location: "+newBL.toString());

            // set new position
            cpPosition = newPos;
            controlLV.setX((float) LayoutLocationMap.get(newPos).getX());
            controlLV.setY((float) LayoutLocationMap.get(newPos).getY());
            controlLV.getLocationOnScreen(location);
            ltag("After:  x: "+location[0]+"  y: "+location[1]);

            // test X to provide location in runtime
            crossDotIV.setX((float) LayoutLocationMap.get(newPos).getX()-20);
            crossDotIV.setY((float) LayoutLocationMap.get(newPos).getY()-20);

        }
    }

    private position getPosition(float pitch, float roll, float v1, float v3) {
        boolean landscape = false;
        if (pitch > -0.5f && pitch < 0.5f) {
            if (roll < 0)
                return position.BottomLeft;
            else
                return position.TopRight;
        }

        if (v3 > 0)
             return position.BottomRight;
        else
            return position.TopLeft;
    }


    private float[] getMatrix(float[] mAccelerometerData, float[] mMagnetometerData) {
        float[] rotationMatrix = new float[9];
        boolean rotationOK = SensorManager.getRotationMatrix(rotationMatrix,
                null, mAccelerometerData, mMagnetometerData);
        float orientationValues[] = new float[3];
        if (rotationOK) {
            SensorManager.getOrientation(rotationMatrix, orientationValues);
            return orientationValues;
        } else {
            return null;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    
    //endregion implements Sensor Implementation 

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void showInfo(String info) {
        infoTV.setVisibility(View.VISIBLE);
        infoTV.setText(info);
    }

    @Override
    public void hideInfo() {
        infoTV.setVisibility(View.INVISIBLE);
    }

}
