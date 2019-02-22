package com.huang.homan.camera2.View.Activity;

import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class CameraActivityAndroidTest {

    @Rule
    public ActivityTestRule<CameraActivity> cameraActivityTestRule =
            new ActivityTestRule<>(CameraActivity.class, true, false);

    @Before
    public void init(){
        cameraActivityTestRule.getActivity()
                .getSupportFragmentManager().beginTransaction();
    }

    @Test
    public void TestAutoComplete(){
        cameraActivityTestRule.launchActivity(null);

    }
}