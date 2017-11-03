package com.example.bdxk.lightvideorecord.utils;

import android.os.Handler;
import android.os.Looper;

public class ThreadHelper {
    public static final Handler handler = new Handler(Looper.getMainLooper());

    public ThreadHelper() {
    }

    public static boolean isMainThread() {
        return Looper.myLooper() == Looper.getMainLooper();
    }

    public static Thread runThread(Runnable var0) {
        Thread var1;
        (var1 = new Thread(var0)).start();
        return var1;
    }

    public static void post(Runnable var0) {
        if(var0 != null) {
            handler.post(var0);
        }
    }

    public static void postDelayed(Runnable var0, long var1) {
        if(var0 != null) {
            handler.postDelayed(var0, var1);
        }
    }

    public static void cancel(Runnable var0) {
        if(var0 != null) {
            handler.removeCallbacks(var0);
        }
    }
}