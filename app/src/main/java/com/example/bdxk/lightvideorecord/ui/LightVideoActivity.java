package com.example.bdxk.lightvideorecord.ui;

import android.content.Context;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bdxk.lightvideorecord.R;
import com.example.bdxk.lightvideorecord.utils.CameraPreviewSurface;
import com.example.bdxk.lightvideorecord.utils.DateUtils;
import com.example.bdxk.lightvideorecord.utils.UriUtils;
import com.example.bdxk.lightvideorecord.utils.VideoUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

    private Context context;

    private CameraPreviewSurface cameraPreviewSurface;

    private MediaRecorder mMediaRecorder;
    private String videoUrlString;//当前视频的位置
    private String saveUrlString = "";//保存视频的最终路径

    private boolean isRecoding;//是否正在录制
    private boolean isPuse;//是否暂停
    private static final String TAG = "--LightVideoActivity--";


    private Handler mHandler;

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            mHandler.postDelayed(runnable, 100);
            addTimeUsed();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_light_video);
        ButterKnife.bind(this);
        context = this;
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
                startOrPause();
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

    private void startOrPause(){
        if (isRecoding) {
            if (isPuse) {
                if (cameraPreviewSurface.prepareVideoRecorder()) {
                    mMediaRecorder.start();
                    runnable.run();//开始计时
                    isPuse = false;
                } else {
                    Toast.makeText(context, "相机初始化失败", Toast.LENGTH_SHORT).show();
                }
            } else {
                cameraPreviewSurface.stopRecorder();
                isPuse = true;
                mHandler.removeCallbacks(runnable);
                if ("".equals(saveUrlString)) {
                    saveUrlString = videoUrlString;
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String[] str = new String[]{saveUrlString, videoUrlString};
                                String saveUrl = UriUtils.getOutputMediaFile();
                                VideoUtils.appendVideo(context, saveUrl, str);
                                File reName = new File(saveUrlString);
                                File f = new File(saveUrl);
                                f.renameTo(reName);
                                if (reName.exists()) {
                                    f.delete();
                                    new File(videoUrlString).delete();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
            }
        } else {
            if (cameraPreviewSurface.prepareVideoRecorder()) {
                mMediaRecorder.start();
                runnable.run();//开始计时
                isRecoding = true;
                tvDel.setVisibility(View.VISIBLE);
                tvFinish.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(context, "相机初始化失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private int longmillTime;//记录的时间长度
    private String timeUsed;

    public void addTimeUsed() {
        longmillTime += 100;
        timeUsed = DateUtils.getMin(longmillTime) + ":" + DateUtils.getSec(longmillTime) + ":" + DateUtils.getLongMill(longmillTime);
        tvUseTime.setText(timeUsed);
    }
}
