package com.travis.recyclerview;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.travis.listviewtest.R;

import java.util.List;

/**
 * Created by yutao on 2018/8/9.
 */

public class RecyclerAdpater extends RecyclerView.Adapter<RecyclerAdpater.ViewHolder> {

    private static final String TAG = "RecyclerAdpater";

    private List<String> list;
    private Context mContext;

    public RecyclerAdpater(Context context, List<String> list) {
        this.list = list;
        this.mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder");
        View view = LayoutInflater.from(mContext).inflate(R.layout.recycler_view_item, null);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder");

        String url = list.get(position);

        ImageView imageView = holder.imageView;
        imageView.setBackgroundResource(R.color.colorGray);
        imageView.setTag(position);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ViewHolder(View view) {
            super(view);
            this.imageView = (ImageView)view.findViewById(R.id.recycler_view_image);
        }

    }
}
