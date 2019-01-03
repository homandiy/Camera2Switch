package com.huang.homan.camera2.MvpHelper;

import com.huang.homan.camera2.View.Fragment.CameraFragment;
import com.huang.homan.camera2.View.common.BaseFragment;

public interface CameraFragmentVP {
    interface View{
        void showInfo(String info);
        void hideInfo();
    }

    interface Presenter {
        void setFragment(CameraFragment cameraFragment);
    }
}
