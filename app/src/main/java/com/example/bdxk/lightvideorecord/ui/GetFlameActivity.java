package com.example.bdxk.lightvideorecord.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bdxk.lightvideorecord.R;
import com.example.bdxk.lightvideorecord.utils.RecyclerAdapter;
import com.example.bdxk.lightvideorecord.utils.UriUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/*
* 获取视频帧并显示--------------测试
* */
public class GetFlameActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    @BindView(R.id.surFaceView)
    SurfaceView surFaceView;
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
    @BindView(R.id.recyclerView)
    RecyclerView recyclerView;

    private Context context;
    private SurfaceHolder surfaceHolder;
    private List<Bitmap> bitmapList = new ArrayList<>();
    private MyHandler myHandler;
    private int w, h;
    private RecyclerAdapter recyclerAdapter;
    private Canvas canvas;

    static class MyHandler extends Handler {
        WeakReference<Activity> mWeakReference;

        public MyHandler(Activity activity) {
            mWeakReference = new WeakReference<Activity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final GetFlameActivity activity = (GetFlameActivity) mWeakReference.get();
            if (activity != null) {
                if (msg.what == 1) {
                    Toast.makeText(activity, "加载完成", Toast.LENGTH_SHORT).show();
                    Bitmap bitmap = activity.zoomImg(activity.bitmapList.get(0), activity.w, activity.h);
                    activity.canvas = activity.surfaceHolder.lockCanvas();
                    activity.canvas.drawBitmap(bitmap, 0, 0, new Paint(Paint.ANTI_ALIAS_FLAG));
                    activity.surfaceHolder.unlockCanvasAndPost(activity.canvas);
                }
                activity.recyclerAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_flame);
        ButterKnife.bind(this);
        this.context = this;
        surfaceHolder = surFaceView.getHolder();
        surfaceHolder.addCallback(this);
        myHandler = new MyHandler(this);

        recyclerAdapter = new RecyclerAdapter(context,bitmapList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(context,LinearLayoutManager.HORIZONTAL,false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(recyclerAdapter);
        recyclerAdapter.setOnItemClick(new RecyclerAdapter.OnItemClick() {
            @Override
            public void click(int position, Bitmap bitmap) {
                canvas = surfaceHolder.lockCanvas();
                canvas.drawBitmap(zoomImg(bitmapList.get(position),w,h), 0, 0, new Paint(Paint.ANTI_ALIAS_FLAG));
                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        });
    }

    @OnClick(R.id.tvImport)
    public void onViewClicked() {
        Intent pickIntent = new Intent(Intent.ACTION_GET_CONTENT);
        pickIntent.setType("video/*");
        pickIntent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(pickIntent, 1);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) return;
        Uri selectedMediaUri = data.getData();
        final String path = UriUtils.getFileAbsolutePath(getApplicationContext(), selectedMediaUri);
        if (!TextUtils.isEmpty(path)) {
            Toast.makeText(context, "加载中---" + path, Toast.LENGTH_SHORT).show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    getFlameBitmap(path, 100);
                }
            }).start();
        } else {
            //视频路径为空
            Toast.makeText(context, "视频路径为空", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        w = width;
        h = height;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        myHandler.removeCallbacks(null);
    }

    //获取视频帧
    private List<Bitmap> getFlameBitmap(String path, long timeMs) {
        bitmapList.clear();
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(path);
        String fileLength = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long vLong = Long.parseLong(fileLength);
        timeMs = vLong / 14;//取15张
        for (int i = 0; i < vLong; i += timeMs) {
            Bitmap bitmap = retriever.getFrameAtTime(i * 1000, MediaMetadataRetriever.OPTION_CLOSEST);
            bitmapList.add(bitmap);
            myHandler.sendEmptyMessage(i==0?1:2);
        }
        return bitmapList;
    }

    public Bitmap zoomImg(Bitmap bm, int newWidth, int newHeight) {
        // 获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 计算缩放比例
        float scaleWidth = ((float) newWidth) / width;
        //float scaleHeight = ((float) newHeight) / height;
        float scaleHeight = scaleWidth;
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 得到新的图片
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
        return newbm;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myHandler.removeCallbacks(null);
    }
}
