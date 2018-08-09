package com.travis.recyclerview;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.travis.listviewtest.DiskLruCache;
import com.travis.listviewtest.ListViewAdapterWithCache;
import com.travis.listviewtest.R;
import com.travis.listviewtest.Util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by yutao on 2018/8/9.
 */

public class RecyclerAdpater extends RecyclerView.Adapter<RecyclerAdpater.ViewHolder> {

    private static final String TAG = "RecyclerAdpater";

    private List<String> list;
    private Context mContext;
    private DiskLruCache diskLruCache;

    private Set<RecyclerAsyncTask> tasks;

    private RecyclerView mRecyclerView;
    private int reqWidth = 100;
    private int reqHeight = 100;

    public RecyclerAdpater(Context context, List<String> list, RecyclerView mRecyclerView, DiskLruCache diskLruCache) {
        this.list = list;
        this.mContext = context;
        this.diskLruCache = diskLruCache;
        this.mRecyclerView = mRecyclerView;

        tasks = new HashSet<RecyclerAsyncTask>();
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

        DiskLruCache.Snapshot snapshot;

        String url = list.get(position);

        ImageView imageView = holder.imageView;
        imageView.setTag(position);

        String key = Util.md5(list.get(position));
        try {
            snapshot = diskLruCache.get(key);
            if (snapshot != null){
                FileInputStream in;
                Bitmap bitmap;

                // 下面的0是指key对应的第1个缓存文件。对应的是创建DiskLRUCache的时候的第三个参数 {
                in = (FileInputStream)snapshot.getInputStream(0);
                bitmap = Util.decodeSampleBitmapFormStream(in, reqWidth, reqHeight);
                // }
                imageView.setImageBitmap(bitmap);
            }else {
                imageView.setImageBitmap(null);
                imageView.setBackgroundResource(R.color.colorGray);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadImage(String url, String key, int index){
        DiskLruCache.Snapshot snapshot = null;
        ImageView imageView;
        FileInputStream in;
        Bitmap bitmap;

        try {
            snapshot = diskLruCache.get(key);
            if (snapshot != null){// 非空表示存在缓存文件
                imageView = (ImageView)mRecyclerView.findViewWithTag(index);

                // 下面的0是指key对应的第1个缓存文件。对应的是创建DiskLRUCache的时候的第三个参数 {
                in = (FileInputStream)snapshot.getInputStream(0);
                bitmap = Util.decodeSampleBitmapFormStream(in, reqWidth, reqHeight);

                if (imageView != null){
                    imageView.setImageBitmap(bitmap);
                }

            }else {
                RecyclerAsyncTask task = new RecyclerAsyncTask(mRecyclerView, diskLruCache, index);
                tasks.add(task);
                task.execute(url);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public void cancleTask() {
        for (RecyclerAsyncTask task : tasks){
            task.cancel(false);
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ViewHolder(View view) {
            super(view);
            this.imageView = (ImageView)view.findViewById(R.id.recycler_view_image);
        }
    }


    private class RecyclerAsyncTask extends AsyncTask<String, Void, Bitmap>{

        //private DiskLruCache diskLruCache;
        private int index;

        public RecyclerAsyncTask(RecyclerView mRecyclerView, DiskLruCache diskLruCache, int index) {
            this.index = index;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            if (this.isCancelled()) return null;

            DiskLruCache.Snapshot snapshot;
            DiskLruCache.Editor editor;
            OutputStream out;
            FileInputStream in;
            Bitmap bitmap=null;

            String url = strings[0];
            String key = Util.md5(url);

            try {
                editor = diskLruCache.edit(key);
                out = editor.newOutputStream(0);
                if (Util.toStream(url, out)){
                    editor.commit();
                }else {
                    editor.abort();
                }
                diskLruCache.flush();

                snapshot = diskLruCache.get(key);
                if (snapshot != null){
                    in = (FileInputStream) snapshot.getInputStream(0);
                    bitmap = Util.decodeSampleBitmapFormStream(in, reqWidth, reqHeight);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {

            if (bitmap != null){
                ImageView imageView = mRecyclerView.findViewWithTag(index);

                imageView.setImageBitmap(bitmap);
            }

            tasks.remove(this);
        }
    }
}
