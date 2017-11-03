package com.example.bdxk.lightvideorecord.utils;

/**
 * Created by BDXK on 2017/11/3.
 */

public class DateUtils {
    /**得到分*/
    public static String getMin(long longmillTime) {
        long min = (longmillTime) / 60000;
        return min < 10 ? "0" + min : min + "";
    }
    /**得到秒*/
    public static String getSec(long longmillTime) {
        long sec = (longmillTime / 1000) % 60;
        return sec < 10 ? "0" + sec : sec + "";
    }
    /**得到0.1秒*/
    public static String getLongMill(long longmillTime) {
        long longmill = (longmillTime / 100) % 10;
        return longmill + "";
    }
}
