package com.example.bdxk.lightvideorecord.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CameraPreviewSurface extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "--CameraPreview---";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private String videoUrlString;//当前视频的位置
    private String saveUrlString = "";//保存视频的最终路径
    private Camera.Size bestSupportSize;//最佳支持的分辨率
    private WrapCameraSize bestWrapCameraSize;//最佳合适的最小分辨率
    private ViewGroup.LayoutParams layoutParams;
    private int cH,cW;//相机预览高宽
    private int w,h;
    private Context context;
    private boolean isCreate;
    private int faceType;//相机前置与后置

    public CameraPreviewSurface(Context context) {
        super(context);
        this.context = context;
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setKeepScreenOn(true);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        w = DeviceUtils.getDisplay(context).widthPixels;
        h = DeviceUtils.getDisplay(context).heightPixels;
        cH = 4*w/3;
        cW = w;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        isCreate = true;
        layoutParams = getLayoutParams();
        layoutParams.height = cH;
        setLayoutParams(layoutParams);
        startPreView(faceType);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        isCreate = false;
        // empty. Take care of releasing the Camera preview in your activity.
        stopCamera();
    }

    /**
     * @param type 设置相机前置后置状态
     */
    public void setFaceType(int type){
        if (isCreate){
            faceType = type;
            startPreView(type==1?Camera.CameraInfo.CAMERA_FACING_FRONT:Camera.CameraInfo.CAMERA_FACING_BACK);
        }
    }

    /**
     * @return  相机前置后置状态
     */
    public int getFaceType() {
        return faceType;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        cW = w;
        cH = h;
        if (mHolder.getSurface() == null){
          return;
        }
        try {
            if (mCamera==null){
                startPreView(faceType);
            }
        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    /**
     * 开启预览
     */
    private void startPreView(int type) {
        Log.d(TAG, "startPreView: ");
        if (mCamera != null) {
            stopCamera();
        }
        //默认启动后置摄像头
        mCamera = Camera.open(type);
        if (mCamera == null) {
            Log.e(TAG,"未能获取到相机！");
            return;
        }
        try {
            mCamera.setPreviewDisplay(mHolder);
            //配置CameraParams
            setCameraParams();
            //启动相机预览
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
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
     * 设置摄像头为竖屏
     */
    private void setCameraParams() {
        if (mCamera != null) {
            Camera.Parameters params = mCamera.getParameters();
            //设置相机的横竖屏(竖屏需要旋转90°)
            //设置聚焦模式
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            //缩短Recording启动时间
            params.setRecordingHint(true);
            //影像稳定能力
            if (params.isVideoStabilizationSupported())
                params.setVideoStabilization(true);
            setCameraDisplayOrientation((Activity)context,Camera.CameraInfo.CAMERA_FACING_BACK,mCamera);
            setCameraSize(mCamera.getParameters(),cW, cH);
            mCamera.setParameters(params);
        } else {
            Log.e(TAG,"未找到相机！");
        }
    }

    /**
     * 停止Camera
     */
    private void stopCamera() {
        if (mCamera != null) {
            mCamera.lock();
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }


    /**
     * 设置相机方向---旋转时
     * @param activity
     * @param cameraIo
     * @param camera
     */
    public void setCameraDisplayOrientation(Activity activity, int cameraIo, Camera camera){
        Camera.CameraInfo info=new Camera.CameraInfo();
        Camera.getCameraInfo(cameraIo,info);
        int rotation=activity.getWindowManager().getDefaultDisplay().getRotation();
        int degress=0;
        switch(rotation){
            case Surface.ROTATION_0:
                degress=0;
                break;
            case Surface.ROTATION_90:
                degress=90;
                break;
            case Surface.ROTATION_180:
                degress=180;
                break;
            case Surface.ROTATION_270:
                degress=270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degress) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degress + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    /**
     * 设置相机尺寸
     * @param parameters
     * @param width
     * @param height
     */
    public void setCameraSize(Camera.Parameters parameters, int width, int height) {
        Map<String, List<Camera.Size>> allSizes = new HashMap<>();
        String typePreview = "typePreview";
        String typePicture = "typePicture";
        allSizes.put(typePreview, parameters.getSupportedPreviewSizes());
        allSizes.put(typePicture, parameters.getSupportedPictureSizes());
        Iterator iterator = allSizes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<Camera.Size>> entry = (Map.Entry<String, List<Camera.Size>>) iterator.next();
            List<Camera.Size> sizes = entry.getValue();
            if (sizes == null || sizes.isEmpty()) continue;
            ArrayList<WrapCameraSize> wrapCameraSizes = new ArrayList<>(sizes.size());
            for (Camera.Size size : sizes) {
                WrapCameraSize wrapCameraSize = new WrapCameraSize();
                wrapCameraSize.setWidth(size.width);
                wrapCameraSize.setHeight(size.height);
                wrapCameraSize.setD(Math.abs((size.width - width)) + Math.abs((size.height - height)));
                if (size.width == width && size.height == height) {
                    bestSupportSize = size;
                    if (typePreview.equals(entry.getKey())) {
                        parameters.setPreviewSize(size.width, size.height);
                    } else if (typePicture.equals(entry.getKey())) {
                        parameters.setPictureSize(size.width, size.height);
                    }
                    Log.d(TAG, "best size: width=" + size.width + ";height=" + size.height);
                    break;
                }
                wrapCameraSizes.add(wrapCameraSize);
            }
            Log.d(TAG, "wrapCameraSizes.size()=" + wrapCameraSizes.size());
            Camera.Size resultSize = null;
            if (typePreview.equals(entry.getKey())) {
                resultSize = parameters.getPreviewSize();
            } else if (typePicture.equals(entry.getKey())) {
                resultSize = parameters.getPictureSize();
            }
            if (resultSize == null || (resultSize.width != width && resultSize.height != height)) {
                //找到相机Preview Size 和 Picture Size中最适合的大小
                if(wrapCameraSizes.isEmpty()) continue;
                WrapCameraSize minCameraSize = Collections.min(wrapCameraSizes);
                while (!(minCameraSize.getWidth() >= width && minCameraSize.getHeight() >= height)) {
                    wrapCameraSizes.remove(minCameraSize);
                    if(wrapCameraSizes.isEmpty()) break;
                    minCameraSize = null;
                    minCameraSize = Collections.min(wrapCameraSizes);
                    bestWrapCameraSize = minCameraSize;
                }
                Log.d(TAG, "best min size: width=" + minCameraSize.getWidth() + ";height=" + minCameraSize.getHeight());
                if (typePreview.equals(entry.getKey())) {
                    parameters.setPreviewSize(minCameraSize.getWidth(), minCameraSize.getHeight());
                } else if (typePicture.equals(entry.getKey())) {
                    parameters.setPictureSize(minCameraSize.getWidth(), minCameraSize.getHeight());
                }
            }
            iterator.remove();
        }
    }
}