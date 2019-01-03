package com.huang.homan.camera2.MvpHelper;

import com.huang.homan.camera2.View.common.BaseFragment;

public interface BaseActivityVP {

    interface View{
        void setFragment(BaseFragment fragment);
    }

    // Handle broadcast receiver
    interface Presenter {
        void addFragment(BaseFragment fragment);
    }
}
