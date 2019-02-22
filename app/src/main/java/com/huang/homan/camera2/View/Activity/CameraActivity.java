package com.huang.homan.camera2.View.Activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.huang.homan.camera2.MvpHelper.BaseActivityVP;
import com.huang.homan.camera2.Presenter.CameraFragmentPresenter;
import com.huang.homan.camera2.R;
import com.huang.homan.camera2.View.Fragment.CameraFragment;
import com.huang.homan.camera2.View.common.BaseFragment;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CameraActivity extends AppCompatActivity
        implements CameraFragment.OnFragmentInteractionListener,
                   BaseActivityVP.View {

    /* Log tag and shortcut */
    final static String TAG = "MYLOG CameraActivity";

    public static void ltag(String message) {
        Log.i(TAG, message);
    }

    /* Toast shortcut */
    public static void msg(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    // Fragment
    @BindView(R.id.camera_container)
    FrameLayout cameraContainer;

    FragmentManager fragmentManager;
    FragmentTransaction ft;
    CameraFragmentPresenter cameraFragmentPresenter;

    /*
    // Action bar
    @BindView(R.id.appbar)
    Toolbar appbar;
    @BindView(R.id.includeBar)
    View includeBar;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the language_menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.appbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch_camera (item.getItemId()) {
            case R.id.rotation:
                //your action
                if (portraitScreen) {
                    // Landscape mode
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                    portraitScreen = false;
                } else {
                    // Portrait mode
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                    portraitScreen = true;
                }

                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }
    */

    public int frameHeight;
    public int frameWidth;

    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);

        // App Bar: Language Menu
        //setSupportActionBar(appbar);
        //appbar.inflateMenu(R.menu.appbar_menu);

        // Fragment
        fragmentManager = getSupportFragmentManager();
        cameraFragmentPresenter = new CameraFragmentPresenter(this);
    }

    @Override
    public void setFragment(BaseFragment fragment) {
        cameraContainer.removeAllViews();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        }

        //showing the presenter on screen
        ft = getSupportFragmentManager().beginTransaction();

        if (fragment.isAdded()) {
            ft.detach(fragment);
            ft.attach(fragment);
        } else {
            ft.replace(R.id.camera_container, fragment);
        }

        ft.addToBackStack(null);
        ft.commitAllowingStateLoss();
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
    }
}
