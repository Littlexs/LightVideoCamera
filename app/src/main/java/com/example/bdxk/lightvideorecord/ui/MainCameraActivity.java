package com.example.bdxk.lightvideorecord.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bdxk.lightvideorecord.R;
import com.example.bdxk.lightvideorecord.utils.DateUtils;
import com.example.bdxk.lightvideorecord.utils.DeviceUtils;
import com.example.bdxk.lightvideorecord.utils.UriUtils;
import com.example.bdxk.lightvideorecord.utils.VideoUtils;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
    private String saveUrlString = "";//保存视频的最终路径
    private String selectPath="";//选择的视频地址
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
        layoutEdit = (RelativeLayout) layoutInflater.inflate(R.layout.recod_edit, null);
        surfaceHolder = surface.getHolder();
        surfaceHolder.setKeepScreenOn(true);
        surfaceHolder.addCallback(this);

        mHandler = new Handler();
        surfaceHandler = new MyHandler(this);

    }

    private Handler mHandler,surfaceHandler;

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
                if (isRecoding) {
                    if (isPuse) {
                        if (prepareVideoRecorder()) {
                            tvStart.setText("正在录制");
                            mMediaRecorder.start();
                            runnable.run();//开始计时
                            isPuse = false;
                        } else {
                            Toast.makeText(context, "相机初始化失败", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        tvStart.setText("已暂停");
                        stopRecorder();
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
                } else {
                    clearSurfaceDraw();
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
        surfaceHolder.setFormat(PixelFormat.OPAQUE);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        Log.i(TAG," ----------- surfaceCreated  "+surfaceHolder);
        if ("".equals(selectPath)){
            startPreView();
        }else {
            Toast.makeText(context, "加载中...", Toast.LENGTH_SHORT).show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    bitmapList = getFlameBitmap(selectPath,100);
                    surfaceHandler.sendEmptyMessage(1);
                }
            }).start();
        }
    }

    private List<Bitmap> bitmapList;
    static class MyHandler extends Handler{
        WeakReference<MainCameraActivity> mActivity;
        public MyHandler(MainCameraActivity activity) {
            mActivity = new WeakReference<MainCameraActivity>(activity);
        }
        @Override
        public void handleMessage(Message msg) {
            MainCameraActivity mainActivity = mActivity.get();
            if(msg.what==1){
                Canvas canvas = mainActivity.surface.getHolder().lockCanvas();
                if (mainActivity.bitmapList==null || mainActivity.bitmapList.size()==0){
                    Toast.makeText(mainActivity, "获取缩略图失败！", Toast.LENGTH_SHORT).show();
                }else {
                    canvas.drawBitmap(mainActivity.bitmapList.get(2),0,0,new Paint(Paint.ANTI_ALIAS_FLAG));
                    mainActivity.surfaceHolder.unlockCanvasAndPost(canvas);
                }
                Toast.makeText(mainActivity, "加载完成", Toast.LENGTH_SHORT).show();
            }
        }
    };
    List<Camera.Size> sizeList;
    Camera.Size optimalSize;
    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Log.i(TAG," ----------- surfaceChanged  "+surfaceHolder);
        if (surfaceHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }
        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
        }
        optimalSize = getOptimalPreviewSize(sizeList, surface.getWidth(), surface.getHeight());
        Camera.Parameters params = mCamera.getParameters();
        params.setPreviewSize(optimalSize.width, optimalSize.height);
        startPreView();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.i(TAG," ----------- surfaceDestroyed  "+surfaceHolder);
        stopRecorder();
        stopCamera();
    }

    /**
     * 开启预览
     */
    private void startPreView() {
        Log.d(TAG, "startPreView: ");
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
        } catch (IOException e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    /**
     * 设置摄像头为竖屏
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
            sizeList = getSupportedVideoSizes(mCamera);
            supportSize = sizeList.get(sizeList.size() / 2);//获取支持分辨率的中间值，个人做法
            mCamera.setParameters(params);
        } else {
            Toast.makeText(context, "未找到相机", Toast.LENGTH_SHORT).show();
        }
    }
    //这里是预览图尺寸处理的方法，就是在这把宽高调换，就可以达到效果
    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;
        if (sizes == null)
            return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Camera.Size size : sizes) {
//                double ratio = (double) size.width / size.height;
            double ratio = (double) size.height / size.width;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.width - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.width - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the
        // requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.width - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.width - targetHeight);
                }
            }
        }

        return optimalSize;
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

    @OnClick({R.id.tvFinish, R.id.tvDel,R.id.tvImport})
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
                if (!isPuse) {
                    Log.i(TAG, "   直接结束");
                    if (!"".equals(saveUrlString)) {
                        Log.i(TAG, saveUrlString);
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
                    }else {
                        saveUrlString = videoUrlString;
                    }
                } else {
                    Log.i(TAG, "   暂停中结束");
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
                if (isRecoding) {
                    stopRecorder();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (!"".equals(saveUrlString)) {
                                File reName = new File(saveUrlString);
                                if (reName.exists()) {
                                    reName.delete();
                                }
                            }
                            new File(videoUrlString).delete();
                            saveUrlString = "";
                        }
                    }).start();
                    tvStart.setText("开始");
                    mHandler.removeCallbacks(runnable);
                    tvUseTime.setText("00:00:0");
                    tvFinish.setVisibility(View.GONE);
                    tvDel.setVisibility(View.GONE);
                }
                break;
            case R.id.tvImport:
                if (isRecoding){
                    stopRecorder();
                    stopCamera();
                }
                Intent pickIntent = new Intent(Intent.ACTION_GET_CONTENT);
                pickIntent.setType("video/*");
                pickIntent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(pickIntent, 1);
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(resultCode != RESULT_OK) return;
        Uri selectedMediaUri = data.getData();
        String path = UriUtils.getFileAbsolutePath(getApplicationContext(),selectedMediaUri);
        if(!TextUtils.isEmpty(path)) {
            Log.i(TAG," ------ selectPath "+path);
            selectPath = path;
        } else {
            //视频路径为空
            Toast.makeText(context,"视频路径为空",Toast.LENGTH_SHORT).show();
        }
    }

    //获取视频帧
    private List<Bitmap> getFlameBitmap(String path,long timeMs){
        List<Bitmap> bitmapList = new ArrayList<>();
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(path);
        String fileLength = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        Log.i(TAG, "fileLength : "+fileLength);
        long vLong = Long.parseLong(fileLength);
        timeMs = vLong/14;//取10张
        for (int i = 0;i<vLong;i+=timeMs){
            Bitmap bitmap = retriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST);
            bitmapList.add(bitmap);
        }
        return bitmapList;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(runnable);
        surfaceHandler.removeCallbacks(null);
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
     *
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
        return eis.getPath() + File.separator +"VID_" + timeStamp + ".mp4";
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

    /**
     * 停止Camera
     */
    private void stopCamera() {
        if (mCamera != null) {
            mCamera.lock();
            try {
                mCamera.setPreviewDisplay(null);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void clearSurfaceDraw(){
        Canvas canvas = null;
        try{
            canvas = surfaceHolder.lockCanvas(null);
            canvas.drawColor(Color.WHITE);
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.SRC);
        }catch(Exception e){

        }finally{
            if(canvas != null){
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }
}
