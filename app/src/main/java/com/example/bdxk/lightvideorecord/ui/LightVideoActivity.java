package com.example.bdxk.lightvideorecord.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.bdxk.lightvideorecord.R;
import com.example.bdxk.lightvideorecord.utils.CameraPreviewSurface;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LightVideoActivity extends AppCompatActivity {

    @BindView(R.id.frameLayout)
    FrameLayout frameLayout;
    @BindView(R.id.tvUseTime)
    TextView tvUseTime;
    @BindView(R.id.imgStart)
    ImageView imgStart;
    @BindView(R.id.tvFinish)
    TextView tvFinish;
    @BindView(R.id.tvDel)
    TextView tvDel;
    @BindView(R.id.tvImport)
    TextView tvImport;
    @BindView(R.id.layout_recod)
    RelativeLayout layoutRecod;

    private CameraPreviewSurface cameraPreviewSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_video);
        ButterKnife.bind(this);
        init();
    }

    private void init() {
        cameraPreviewSurface = new CameraPreviewSurface(this);
        frameLayout.removeAllViews();
        frameLayout.addView(cameraPreviewSurface);
        imgStart.setSelected(false);
    }

    @OnClick({R.id.imgStart, R.id.tvFinish, R.id.tvDel, R.id.tvImport,R.id.imgChangeFace})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.imgStart://开始录制
                imgStart.setSelected(!imgStart.isSelected());
                break;
            case R.id.tvFinish://完成
                break;
            case R.id.tvDel://删除录制或者导入的视频
                break;
            case R.id.tvImport://导入视频
                break;
            case R.id.imgChangeFace://旋转相机镜头
                cameraPreviewSurface.setFaceType(cameraPreviewSurface.getFaceType()==1?0:1);
                break;
        }
    }
}
