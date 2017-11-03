package com.example.bdxk.lightvideorecord.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bdxk.lightvideorecord.R;
import com.example.bdxk.lightvideorecord.utils.DateUtils;
import com.example.bdxk.lightvideorecord.utils.DeviceUtils;
import com.example.bdxk.lightvideorecord.utils.VideoUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainCameraActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    @BindView(R.id.rootLayout)
    LinearLayout rootLayout;
    @BindView(R.id.layout_recod)
    RelativeLayout layoutRecod;
    @BindView(R.id.tvStart)
    TextView tvStart;
    @BindView(R.id.bottomLayout)
    FrameLayout bottomLayout;
    @BindView(R.id.surFaceView)
    SurfaceView surface;

    RelativeLayout layoutEdit;
    LayoutInflater layoutInflater;

    Context context;
    @BindView(R.id.tvFinish)
    TextView tvFinish;
    @BindView(R.id.tvDel)
    TextView tvDel;
    @BindView(R.id.tvUseTime)
    TextView tvUseTime;

    private Camera mCamera;
    private MediaRecorder mMediaRecorder;
    //屏幕分辨率相关
    private DisplayMetrics displayMetrics;
    private int videoWidth, videoHeight;

    private SurfaceHolder surfaceHolder;

    private String videoUrlString;//当前视频的位置
    private String saveUrlString="";//保存视频的最终路径
    private Camera.Size supportSize;//设置支持的分辨率

    private boolean isRecoding;//是否正在录制
    private boolean isPuse;//是否暂停

    private static final String TAG = "--MainCameraActivity--";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_camera);
        ButterKnife.bind(this);
        context = getApplicationContext();
        layoutInflater = LayoutInflater.from(this);
        displayMetrics = DeviceUtils.getDisplay(context);
        videoWidth = displayMetrics.widthPixels;
        videoHeight = displayMetrics.heightPixels;
        init();
    }

    /**
     * 初始化
     */
    private void init() {
        layoutEdit = (RelativeLayout) layoutInflater.inflate(R.layout.recod_edit, null, false);
        surfaceHolder = surface.getHolder();
        surfaceHolder.setFixedSize(videoWidth, videoHeight);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder.setKeepScreenOn(true);
        surfaceHolder.addCallback(this);

        mHandler = new Handler();
    }

    private Handler mHandler;

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            mHandler.postDelayed(runnable, 100);
            addTimeUsed();
        }
    };

    private int longmillTime;//记录的时间长度
    private String timeUsed;

    public void addTimeUsed() {
        longmillTime += 100;
        timeUsed = DateUtils.getMin(longmillTime) + ":" + DateUtils.getSec(longmillTime) + ":" + DateUtils.getLongMill(longmillTime);
        tvUseTime.setText(timeUsed);
    }


    @OnClick({R.id.tvStart})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.tvStart:
                //开始录制和暂停
                if (isRecoding){
                    if (isPuse){
                        if (prepareVideoRecorder()) {
                            tvStart.setText("正在录制");
                            mMediaRecorder.start();
                            runnable.run();//开始计时
                            isPuse = false;
                        }else {
                            Toast.makeText(context, "相机初始化失败", Toast.LENGTH_SHORT).show();
                        }
                    }else {
                        tvStart.setText("已暂停");
                        mCamera.autoFocus(new Camera.AutoFocusCallback() {
                            @Override
                            public void onAutoFocus(boolean success, Camera camera) {
                                if (success == true)
                                    mCamera.cancelAutoFocus();
                            }
                        });
                        stopRecorder();
                        isPuse = true;
                        mHandler.removeCallbacks(runnable);
                        if ("".equals(saveUrlString)){
                            saveUrlString = videoUrlString;
                        }else {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        String[] str = new String[]{saveUrlString, videoUrlString};
                                        String saveUrl = getOutputMediaFile();
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
                }else {
                    if (prepareVideoRecorder()) {
                        tvStart.setText("正在录制");
                        mMediaRecorder.start();
                        runnable.run();//开始计时
                        isRecoding = true;
                        tvDel.setVisibility(View.VISIBLE);
                        tvFinish.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(context, "相机初始化失败", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        this.surfaceHolder = surfaceHolder;
        startPreView();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        stopRecorder();
        stopCamera();
    }

    /**
     * 开启预览
     */
    private void startPreView() {
        Log.d(TAG, "startPreView: ");
        stopCamera();
        if (mCamera != null) {
            stopCamera();
        }
        //默认启动后置摄像头
        mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        if (mCamera == null) {
            Toast.makeText(this, "未能获取到相机！", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
            //配置CameraParams
            setCameraParams();
            //启动相机预览
            mCamera.startPreview();
            List<Camera.Size> sizeList = getSupportedVideoSizes(mCamera);
            for (Camera.Size s : sizeList) {
                Log.i(TAG, s.height + "  " + s.width);
            }
            supportSize = sizeList.get(sizeList.size() / 2);
        } catch (IOException e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    /**
     * 设置摄像头为竖屏
     *
     * @author lip
     * @date 2015-3-16
     */
    private void setCameraParams() {
        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            //设置相机的横竖屏(竖屏需要旋转90°)
            if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                params.set("orientation", "portrait");
                mCamera.setDisplayOrientation(90);
            } else {
                params.set("orientation", "landscape");
                mCamera.setDisplayOrientation(0);
            }
            //设置聚焦模式
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            //缩短Recording启动时间
            params.setRecordingHint(true);
            //影像稳定能力
            if (params.isVideoStabilizationSupported())
                params.setVideoStabilization(true);
            mCamera.setParameters(params);
        } else {
            Toast.makeText(context, "未找到相机", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * @param camera
     * @return 获取设备支持的分辨率
     */
    public List<Camera.Size> getSupportedVideoSizes(Camera camera) {
        if (camera.getParameters().getSupportedVideoSizes() != null) {
            return camera.getParameters().getSupportedVideoSizes();
        } else {
            return camera.getParameters().getSupportedPreviewSizes();
        }
    }

    /**
     * 停止Camera
     */
    private void stopCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @OnClick({R.id.tvFinish, R.id.tvDel})
    public void option(View view) {
        switch (view.getId()) {
            case R.id.tvFinish://完成录制
                //结束录制并保存视频
                stopRecorder();
                mCamera.lock();
                stopCamera();
                tvStart.setText("开始");
                longmillTime = 0;
                mHandler.removeCallbacks(runnable);
                if (!isPuse){
                    Log.i(TAG,"   直接结束");
                    if (!"".equals(saveUrlString)){
                        Log.i(TAG,saveUrlString);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    String[] str = new String[]{saveUrlString, videoUrlString};
                                    String saveUrl = getOutputMediaFile();
                                    VideoUtils.appendVideo(context, saveUrl, str);
                                    File reName = new File(saveUrlString);
                                    File f = new File(saveUrl);
                                    f.renameTo(reName);
                                    if (reName.exists()) {
                                        f.delete();
                                        new File(videoUrlString).delete();
                                    }
                                    saveUrlString = "";
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }
                }else {
                    Log.i(TAG,"   暂停中结束");
                }
                isPuse = false;
                isRecoding = false;
//                Animation animation;
//                animation = AnimationUtils.loadAnimation(this, R.anim.buttom_layout_out);
//                animation.setAnimationListener(new Animation.AnimationListener() {
//                    @Override
//                    public void onAnimationStart(Animation animation) {
//
//                    }
//
//                    @Override
//                    public void onAnimationEnd(Animation animation) {
//                        bottomLayout.removeView(layoutRecod);
//                        bottomLayout.addView(layoutEdit);
//                        layoutEdit.startAnimation(AnimationUtils.loadAnimation(context, R.anim.buttom_layout_in));
//                    }
//
//                    @Override
//                    public void onAnimationRepeat(Animation animation) {
//
//                    }
//                });
//                layoutRecod.startAnimation(animation);
                break;
            case R.id.tvDel://删除视频
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(runnable);
    }

    /**
     * 配置MediaRecorder()
     */
    private boolean prepareVideoRecorder() {
        startPreView();
        mMediaRecorder = new MediaRecorder();
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        //2.设置视频，音频的输出格式 mp4
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
        //3.设置音频的编码格式
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        //设置图像的编码格式
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        //音频一秒钟包含多少数据位
        CamcorderProfile mProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        mMediaRecorder.setAudioEncodingBitRate(44100);
        if (mProfile.videoBitRate > 2 * 1024 * 1024)
            mMediaRecorder.setVideoEncodingBitRate(2 * 1024 * 1024);
        else
            mMediaRecorder.setVideoEncodingBitRate(1024 * 1024);
        mMediaRecorder.setVideoFrameRate(mProfile.videoFrameRate);
        //设置选择角度，顺时针方向，因为默认是逆向90度的，这样图像就是正常显示了,这里设置的是观看保存后的视频的角度
        mMediaRecorder.setOrientationHint(90);
        //设置录像的分辨率 ---  一定要是设备支持的分辨路，否则会start失败（-19）
        mMediaRecorder.setVideoSize(supportSize.width, supportSize.height);
        videoUrlString = getOutputMediaFile();
        mMediaRecorder.setOutputFile(videoUrlString);
        mMediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            stopRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            stopRecorder();
            return false;
        }
        return true;
    }

    /**
     * 设置输出文件夹
     * @return
     */
    private String getOutputMediaFile() {
        File sdDir = null;
        boolean sdCardExist = Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
        if (sdCardExist) {
            sdDir = Environment.getExternalStorageDirectory();
        } else if (!sdCardExist) {
            Toast.makeText(context, "SD卡不存在", Toast.LENGTH_SHORT).show();
        }
        File eis = new File(sdDir.toString() + "/BDXK/");
        try {
            if (!eis.exists()) {
                eis.mkdir();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile = new File(eis.getPath() + File.separator +
                "VID_" + timeStamp + ".mp4");
        Log.i(TAG, mediaFile.getAbsolutePath());
        return mediaFile.toString();
    }

    private void stopRecorder() {
        if (mMediaRecorder != null) {
            // 设置后不会崩
            mMediaRecorder.setOnErrorListener(null);
            mMediaRecorder.setPreviewDisplay(null);
            //停止录制
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            //释放资源
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }
}
