package com.huang.homan.camera2.View.Fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;
import io.reactivex.annotations.NonNull;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CameraFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CameraFragment extends BaseFragment
        implements CameraFragmentVP.View {

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

    private int getRotation() {
        return getActivity().getWindowManager().getDefaultDisplay().getRotation();
    }

    Unbinder unbinder;

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
