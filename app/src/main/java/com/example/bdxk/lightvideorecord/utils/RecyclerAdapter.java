package com.example.bdxk.lightvideorecord.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.bdxk.lightvideorecord.R;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by BDXK on 2017/11/6.
 */

public class RecyclerAdapter extends RecyclerView.Adapter {

    private Context context;
    private List<Bitmap> list;

    public RecyclerAdapter(Context context, List<Bitmap> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.liner_item, null);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        ViewHolder viewHolder = (ViewHolder) holder;
        final Bitmap bitmap = zoomImg(list.get(position),0,200);
        viewHolder.image.setImageBitmap(bitmap);
        viewHolder.image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClick!=null){
                    onItemClick.click(position,bitmap);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public interface OnItemClick{
        void click(int position,Bitmap bitmap);
    }

    private OnItemClick onItemClick;

    public void setOnItemClick(OnItemClick onItemClick){
        this.onItemClick = onItemClick;
    }

    public Bitmap zoomImg(Bitmap bm, int newWidth, int newHeight) {
        // 获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 计算缩放比例
        float scaleHeight = ((float) newHeight) / height;
        float scaleWidth = scaleHeight;
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 得到新的图片
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
        return newbm;
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.image)
        ImageView image;
        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this,itemView);
        }
    }
}
