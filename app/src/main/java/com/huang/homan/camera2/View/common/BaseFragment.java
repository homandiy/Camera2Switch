package com.huang.homan.camera2.View.common;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.huang.homan.camera2.R;

import java.util.Locale;


public abstract class BaseFragment extends Fragment {

    /* Log tag and shortcut */
    final static String TAG = "MYLOG BaseFrag";
    public static void ltag(String message) { Log.i(TAG, message); }

    // the root view
    protected View rootView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle
            savedInstanceState) {
        rootView = inflater.inflate(getLayout(), container, false);
        return rootView;
    }

    protected abstract int getLayout();


    @Override
    public void onCreate(Bundle savedInstanceState) {
        setLocale(getSetting(getContext()));
        super.onCreate(savedInstanceState);
    }

    //region implements setting

    private String getPref(String key, Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(
                getString(R.string.camera2_setting), Context.MODE_PRIVATE);
        return sharedPref.getString(key, null);
    }

    private Locale getSetting(Context context) {
        String mLanguage = getPref(getString(R.string.key_language), context);
        Locale locale;

        // Change locale
        if ( mLanguage!=null && mLanguage.equals("zh")) {
            locale = Locale.SIMPLIFIED_CHINESE;
        } else {
            locale = Locale.ENGLISH;
        }
        ltag("Language setting: "+ locale.getDisplayLanguage());

        return locale;
    }

    private void setLocale(Locale locale) {
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.setLocale(locale);
        res.updateConfiguration(conf, dm);
    }

    //endregion implements setting
}
