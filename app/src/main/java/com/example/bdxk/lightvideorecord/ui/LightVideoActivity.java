package com.example.bdxk.lightvideorecord.ui;

import android.app.Activity;
import android.content.Context;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bdxk.lightvideorecord.BaseApp;
import com.example.bdxk.lightvideorecord.R;
import com.example.bdxk.lightvideorecord.utils.AnimatorUtils;
import com.example.bdxk.lightvideorecord.utils.CameraPreviewSurface;
import com.example.bdxk.lightvideorecord.utils.DateUtils;
import com.example.bdxk.lightvideorecord.utils.UriUtils;
import com.example.bdxk.lightvideorecord.utils.VideoUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.example.bdxk.lightvideorecord.utils.UriUtils.getOutputMediaFile;

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
    @BindView(R.id.bottomLayout)
    FrameLayout bottomLayout;

    private Context context;

    private CameraPreviewSurface cameraPreviewSurface;

    private MediaRecorder mMediaRecorder;
    private String videoUrlString;//当前视频的位置
    private String saveUrlString = "";//保存视频的最终路径

    private boolean isRecoding;//是否正在录制
    private boolean isPuse;//是否暂停
    private static final String TAG = "--LightVideoActivity--";
    private View flameView;//帧图片根布局
    private RecyclerView flameRecycler;//帧图片recyclerview

    private Handler mHandler;
    private MyHandler myHandler;

    class MyHandler extends Handler {
        WeakReference<Activity> mWeakReference;
        public MyHandler(Activity activity) {
            mWeakReference = new WeakReference<Activity>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            final LightVideoActivity activity = (LightVideoActivity) mWeakReference.get();
            if (activity != null) {
                if (msg.what == 1) {
                    AnimatorUtils.startAnim(BaseApp.getApplication(), layoutRecod, new AnimatorUtils.AnimListener() {
                        @Override
                        public void animEnd() {
                            bottomLayout.removeView(layoutRecod);
                            flameView = LayoutInflater.from(context).inflate(R.layout.flame_recyclerview_layout, null);
                            flameRecycler = flameView.findViewById(R.id.recyclerView);
                            bottomLayout.addView(flameView);
                            flameView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.buttom_layout_in));
                        }
                    });
                }
            }
        }
    }

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
        mHandler = new Handler();
        myHandler = new MyHandler(this);
    }

    @OnClick({R.id.imgStart, R.id.tvFinish, R.id.tvDel, R.id.tvImport, R.id.imgChangeFace})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.imgStart://开始录制
                imgStart.setSelected(!imgStart.isSelected());
                startOrPause();
                break;
            case R.id.tvFinish://完成
                //结束录制并保存视频
                finishRecoding();
                break;
            case R.id.tvDel://删除录制或者导入的视频
                break;
            case R.id.tvImport://导入视频
                break;
            case R.id.imgChangeFace://旋转相机镜头
                //旋转后，视频录制的是倒立的，同时旋转后的视频合并暂时没解决
                cameraPreviewSurface.setFaceType(cameraPreviewSurface.getFaceType() == 1 ? 0 : 1);
                break;
        }
    }

    private void startOrPause() {
        if (isRecoding) {
            if (isPuse) {
                if (cameraPreviewSurface.prepareVideoRecorder() && cameraPreviewSurface.getmMediaRecorder() != null) {
                    cameraPreviewSurface.getmMediaRecorder().start();
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
                    saveUrlString = cameraPreviewSurface.getVideoUrlString();
                } else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String[] str = new String[]{saveUrlString, cameraPreviewSurface.getVideoUrlString()};
                                String saveUrl = getOutputMediaFile();
                                VideoUtils.appendVideo(context, saveUrl, str);
                                File reName = new File(saveUrlString);
                                File f = new File(saveUrl);
                                f.renameTo(reName);
                                if (reName.exists()) {
                                    f.delete();
                                    new File(cameraPreviewSurface.getVideoUrlString()).delete();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
            }
        } else {
            if (cameraPreviewSurface.prepareVideoRecorder() && cameraPreviewSurface.getmMediaRecorder() != null) {
                cameraPreviewSurface.getmMediaRecorder().start();
                runnable.run();//开始计时
                isRecoding = true;
                tvDel.setVisibility(View.VISIBLE);
                tvFinish.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(context, "相机初始化失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void finishRecoding() {
        imgStart.setSelected(false);
        cameraPreviewSurface.stopRecorder();
        cameraPreviewSurface.stopCamera();
        longmillTime = 0;
        mHandler.removeCallbacks(runnable);
        if (!isPuse) {
            Log.i(TAG, "   直接结束");
            if (!"".equals(saveUrlString)) {
                Log.i(TAG, saveUrlString);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String[] str = new String[]{saveUrlString, cameraPreviewSurface.getVideoUrlString()};
                            String saveUrl = getOutputMediaFile();
                            VideoUtils.appendVideo(context, saveUrl, str);
                            File reName = new File(saveUrlString);
                            File f = new File(saveUrl);
                            f.renameTo(reName);
                            if (reName.exists()) {
                                f.delete();
                                new File(cameraPreviewSurface.getVideoUrlString()).delete();
                            }
                            myHandler.sendEmptyMessage(1);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            } else {
                saveUrlString = cameraPreviewSurface.getVideoUrlString();
            }
        } else {
            Log.i(TAG, "   暂停中结束");
            myHandler.sendEmptyMessage(1);
        }
        isPuse = false;
        isRecoding = false;
    }

    private int longmillTime;//记录的时间长度
    private String timeUsed;

    public void addTimeUsed() {
        longmillTime += 100;
        timeUsed = DateUtils.getMin(longmillTime) + ":" + DateUtils.getSec(longmillTime) + ":" + DateUtils.getLongMill(longmillTime);
        tvUseTime.setText(timeUsed);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(runnable);
        myHandler.removeCallbacks(null);
    }
}
