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

    private TextureView.SurfaceTextureListener mTextureListener =
            new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            presenter.setupCamera(width, height);
            presenter.openCamera(rxPermissions);
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

    // Layout measurement variables
    @BindView(R.id.cameraCL)
    ConstraintLayout cameraCL;
    private int height;
    private int width;
    private int offset;

    @BindView(R.id.infoTV)
    TextView infoTV;

    @BindView(R.id.iv_show)
    ImageView iv_show;

    public void setPicture(Bitmap bitmap) {
        iv_show.setImageBitmap(bitmap);
    }

    @BindView(R.id.captureIV)
    ImageView captureIV;
    private int capButHeight;
    private int capButWidth;
    private position capPosition = position.BottomRight;

    @OnClick(R.id.captureIV)
    public void onViewClicked() {
        ltag("Capture Button Clicked.");

        presenter.playShutter();
        presenter.playShutter2();
        //presenter.capturePhoto(getRotation());
    }

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
                    } else {
                        // Oops permission denied
                        msg(getContext(), "You don't have the permission!");
                        ltag("You don't have the permission!");
                    }
                });

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
        if (height > 0) {
            registerSensors();
            // Reset button position
            captureIV.setX((float) ButtonMap.get(position.BottomRight).getX());
            captureIV.setY((float) ButtonMap.get(position.BottomRight).getY());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
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

        // Get fragment measurement
        ViewTreeObserver vto = cameraCL.getViewTreeObserver();
        vto.addOnGlobalLayoutListener (
                new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                cameraCL.getViewTreeObserver()
                        .removeOnGlobalLayoutListener(this);
                height  = cameraCL.getMeasuredWidth();
                width = cameraCL.getMeasuredHeight();

                ltag("fragment ~ h: "+height+"    w: "+width);

                capButHeight = captureIV.getHeight();
                capButWidth = captureIV.getWidth();

                ltag("capture button ~ h: "+capButHeight+"    w: "+capButWidth);
                int location[] = new int[2];
                captureIV.getLocationOnScreen(location);

                int offset1 = height - location[0];
                int offset2 = width - location[1];
                offset = offset1;
                ltag("capture button ~ x: "+location[0]+", offsetX: "+offset1+"."+
                                  "    y: "+location[1]+", offsetY: "+offset2+".");

                createButtonMap(height, width, capButHeight, capButWidth);

                registerSensors();
            }
        });

        return view;
    }

    public enum position {
        TopLeft, TopRight, BottomLeft, BottomRight
    }

    private class ButtonLocation{
        private int x;
        private int y;

        public ButtonLocation(int x, int y) {
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

    private Map<position, ButtonLocation> ButtonMap = new HashMap<>();

    private void createButtonMap(int x, int y, int cbx, int cby) {
        int gap = offset - cbx;
        // set top left position
        ButtonMap.put(position.TopLeft, new ButtonLocation(gap, gap));
        // set top right position
        ButtonMap.put(position.TopRight, new ButtonLocation((x-offset), gap));
        // set bottom left position
        ButtonMap.put(position.BottomLeft, new ButtonLocation(gap, (y-offset)));
        // set top right position
        ButtonMap.put(position.BottomRight, new ButtonLocation((x-offset), (y-offset)));
    }

    @Override
    public void onViewCreated(@android.support.annotation.NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTextureView.setSurfaceTextureListener(mTextureListener);

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

    //region implements Sensor Implementation 
    /**
     * Sensors change
     */
    private float[] mAccelerometerData = new float[3];
    private float[] mMagnetometerData = new float[3];
    private int angel = 0;
    private position butPosition;

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
                float[] mRotationVectors = sensorEvent.values.clone();
                rvData1.setText(getResources().getString(
                        R.string.float_format, mRotationVectors[0]));
                rvData2.setText(getResources().getString(
                        R.string.float_format, mRotationVectors[1]));
                rvData3.setText(getResources().getString(
                        R.string.float_format, mRotationVectors[2]));
                rvData4.setText(getResources().getString(
                        R.string.float_format, mRotationVectors[3]));
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

            moveCaptureButton(pitch, roll);
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
     * @param pitch
     * @param roll
     */
    private void moveCaptureButton(float pitch, float roll) {

        position newPos = getPosition(pitch, roll);

        if (newPos != capPosition) {
            ltag("Old Pos: "+capPosition.name()+"    --   New Pos: "+newPos.name());


            ButtonLocation oldBL = ButtonMap.get(capPosition);
            ButtonLocation newBL = ButtonMap.get(newPos);
            ltag("Old Location: "+oldBL.toString()+".  New Location: "+newBL.toString());

            capPosition = newPos;
            captureIV.setX((float) ButtonMap.get(newPos).getX());
            captureIV.setY((float) ButtonMap.get(newPos).getY());

            int location[] = new int[2];
            captureIV.getLocationOnScreen(location);
            ltag("Capture Button:  x: "+location[0]+"  y: "+location[1]);

            captureIV.setRotation(0);
            switch (newPos) {
                case TopLeft: captureIV.setRotation(180); break;
                case TopRight:  captureIV.setRotation(-90); break;
                case BottomLeft:  captureIV.setRotation(90); break;
            }
        }
    }

    private position getPosition(float pitch, float roll) {
        boolean landscape = true;
        if (roll > -1.3f && roll < 1.3f) {
            landscape = false;
        }

        if (landscape) {
            if (roll > 0) {
                return position.TopRight;
            }
            return position.BottomLeft;

        } else {
            // portrait: straight up
            if (pitch < 0) {
                return position.BottomRight;
            }
            return position.TopLeft;
        }
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
