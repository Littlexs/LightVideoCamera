package com.example.bdxk.lightvideorecord.ui;

import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bdxk.lightvideorecord.R;
import com.example.bdxk.lightvideorecord.utils.CameraPreview;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class TestActivity extends AppCompatActivity {

    @BindView(R.id.f)
    FrameLayout f;
    @BindView(R.id.tvUseTime)
    TextView tvUseTime;
    @BindView(R.id.tvStart)
    TextView tvStart;
    @BindView(R.id.tvFinish)
    TextView tvFinish;
    @BindView(R.id.tvDel)
    TextView tvDel;
    @BindView(R.id.tvImport)
    TextView tvImport;
    @BindView(R.id.layout_recod)
    RelativeLayout layoutRecod;
    @BindView(R.id.bottomLayout)
    FrameLayout bottomLayout;
    @BindView(R.id.rootLayout)
    LinearLayout rootLayout;

    private static final String TAG = "--TestActivity--";
    Camera mCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        ButterKnife.bind(this);
        CameraPreview cameraPreview = new CameraPreview(this,Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK));
        f.addView(cameraPreview);
    }



    @OnClick({R.id.f, R.id.tvUseTime, R.id.tvStart, R.id.tvFinish, R.id.tvDel, R.id.tvImport, R.id.layout_recod, R.id.bottomLayout, R.id.rootLayout})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.f:
                break;
            case R.id.tvUseTime:
                break;
            case R.id.tvStart:
                break;
            case R.id.tvFinish:
                break;
            case R.id.tvDel:
                break;
            case R.id.tvImport:
                break;
            case R.id.layout_recod:
                break;
            case R.id.bottomLayout:
                break;
            case R.id.rootLayout:
                break;
        }
    }
}
