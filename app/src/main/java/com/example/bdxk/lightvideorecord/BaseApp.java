package com.example.bdxk.lightvideorecord;

import android.app.Application;
import android.content.Context;

/**
 * Created by littlexs on 2017/11/13.
 */

public class BaseApp extends Application {

    private static BaseApp mInstance;

    public static Context getApplication() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }
}
