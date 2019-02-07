package com.huang.homan.camera2.View.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.List;

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

    @BindView(R.id.infoTV)
    TextView infoTV;

    @BindView(R.id.iv_show)
    ImageView iv_show;

    public void setPicture(Bitmap bitmap) {
        iv_show.setImageBitmap(bitmap);
    }

    @BindView(R.id.captureIV)
    ImageView captureIV;

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

    private int getRotation() {
        return getActivity().getWindowManager().getDefaultDisplay().getRotation();
    }

    Unbinder unbinder;

    // Sensor Manager
    private SensorManager mSensorManager;
    private List<Sensor> sensorList = new ArrayList<>();
    private Sensor mRotationSensor;

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
        CameraFragment fragment = new CameraFragment();
        return fragment;
    }

    @SuppressLint("CheckResult")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
            mSensorManager.registerListener(this, mRotationSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } catch (Exception e) {
            msg(getContext(), "Hardware compatibility issue");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ltag("onCreateView()");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(@android.support.annotation.NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTextureView.setSurfaceTextureListener(mTextureListener);

        presenter.setFragment(this);

        getSensorList();
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mRotationSensor, SECOND);
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
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


    private float[] mAccelerometerData = new float[3];
    private float[] mMagnetometerData = new float[3];

    @SuppressLint("DefaultLocale")
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {


        if (sensorEvent.sensor == mRotationSensor) {
            if (sensorEvent.values.length > 4) {
                float[] truncatedRotationVector = new float[4];

                System.arraycopy(sensorEvent.values, 0, truncatedRotationVector, 0, 4);
                rvData1.setText(format("%.2f", truncatedRotationVector[0]));
                rvData2.setText(format("%.2f", truncatedRotationVector[1]));
                rvData3.setText(format("%.2f", truncatedRotationVector[2]));
                rvData4.setText(format("%.2f", truncatedRotationVector[3]));
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

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
