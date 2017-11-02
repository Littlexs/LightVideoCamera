package com.example.bdxk.lightvideorecord.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.bdxk.lightvideorecord.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainCameraActivity extends AppCompatActivity {

    @BindView(R.id.layout_recod)
    RelativeLayout layoutRecod;
    @BindView(R.id.layout_edit)
    RelativeLayout layoutEdit;
    @BindView(R.id.tvStart)
    TextView tvStart;
    @BindView(R.id.tvNext)
    TextView tvNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_camera);
        ButterKnife.bind(this);

    }

    @OnClick({R.id.tvStart, R.id.tvNext})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.tvStart:
                setShowAndHide(layoutRecod,false);
                setShowAndHide(layoutEdit,true);
                break;
            case R.id.tvNext:
                setShowAndHide(layoutRecod,true);
                setShowAndHide(layoutEdit,false);
                break;
        }
    }

    private void setShowAndHide(View view,boolean show){
        Animation animation;
        if (show){
            animation = AnimationUtils.loadAnimation(this, R.anim.buttom_layout_in);
        }else {
            animation = AnimationUtils.loadAnimation(this, R.anim.buttom_layout_out);
        }
        view.startAnimation(animation);
    }
}
