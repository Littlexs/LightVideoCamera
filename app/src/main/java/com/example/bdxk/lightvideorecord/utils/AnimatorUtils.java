package com.example.bdxk.lightvideorecord.utils;

import android.content.Context;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.example.bdxk.lightvideorecord.R;

/**
 * Created by BDXK on 2017/11/15.
 */

public class AnimatorUtils {
    public interface AnimListener{
        void animEnd();
    }
    public static void startAnim(final Context context, View target, final AnimListener listener){
                Animation animation;
                animation = AnimationUtils.loadAnimation(context, R.anim.buttom_layout_out);
                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        listener.animEnd();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
        target.startAnimation(animation);
    }
}
