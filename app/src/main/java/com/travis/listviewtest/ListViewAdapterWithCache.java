package com.travis.listviewtest;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by yutao on 2018/8/3.
 * https://blog.csdn.net/hzw19920329/article/details/51523658
 */

public class ListViewAdapterWithCache extends BaseAdapter {

    public static final String TAG = "ListViewAdapterCache";

    public List<String> list;
    public DiskLruCache diskLruCache;
    public LayoutInflater inflater;
    public ListView listView;
    public Set<ImageAsyncTask> tasks;

    public int reqWidth;
    public int reqHeight;

    public ListViewAdapterWithCache() {
    }

    public ListViewAdapterWithCache(Context context, List<String> list,
                                    DiskLruCache diskLruCache, ListView listView) {
        this.list = list;
        this.diskLruCache = diskLruCache;
        this.listView = listView;

        tasks = new HashSet<ImageAsyncTask>();
        inflater = LayoutInflater.from(context);

        reqHeight = 100;
        reqWidth = 100;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int i) {
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        ViewHolder holder = null;
        DiskLruCache.Snapshot snapshot = null;

        if (convertView == null){
            convertView = inflater.inflate(R.layout.list_item_layout,null);
            holder = new ViewHolder();
            holder.imageView = (ImageView)convertView.findViewById(R.id.item_image);
            holder.textView = (TextView)convertView.findViewById(R.id.item_text);
            convertView.setTag(holder); // 复用holder
        }else {
            holder = (ViewHolder) convertView.getTag();
        }
        // 为ImageView设置标志，防止乱序
        holder.imageView.setTag(position);
        holder.textView.setTag(position + "#");
        Log.d(TAG, "getView");
        try {
            snapshot = diskLruCache.get(Util.md5(list.get(position)));
            if (snapshot != null){ // 缓存中有的话，就从缓存中读取
                FileInputStream in;
                Bitmap bitmap;
                in = (FileInputStream)snapshot.getInputStream(0);
                bitmap = Util.decodeSampleBitmapFormStream(in, reqWidth, reqHeight);
                holder.imageView.setImageBitmap(bitmap);
                holder.textView.setText(list.get(position));
            }else { // 缓存中没有的话，就先置为一个灰色
                holder.imageView.setImageBitmap(null);
                holder.imageView.setBackgroundResource(R.color.colorGray);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return convertView;
    }

    /**
     * 加载图片
     * @param url
     * @param key
     * @param index
     */
    public void loadImage(String url, String key, final int index ){
        final ImageView imageView;
        final TextView textView;
        DiskLruCache.Snapshot snapshot = null;
        FileInputStream in = null;
        Bitmap bitmap = null;

        try {
            snapshot = diskLruCache.get(key);
            if (snapshot != null){// 非空表示存在缓存文件
                imageView = (ImageView)listView.findViewWithTag(index);
                textView = (TextView)listView.findViewWithTag(index + "#");

                // 下面的0是指key对应的第1个缓存文件。对应的是创建DiskLRUCache的时候的第三个参数 {
                in = (FileInputStream)snapshot.getInputStream(0);
                bitmap = Util.decodeSampleBitmapFormStream(in, reqWidth, reqHeight);
                // }

                if (imageView != null){
                    imageView.setImageBitmap(bitmap);
                }

                if (textView != null){
                    textView.setText(list.get(index));
                }
            }else {
                ImageAsyncTask task = new ImageAsyncTask(listView, diskLruCache, index);
                tasks.add(task);
                task.execute(url);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 暂停所有任务
     * 防止滑动的时候仍有线程处于请求状态
     */
    public void cancleTask(){
       if (tasks != null){
           for(ImageAsyncTask task : tasks){
               task.cancel(false);
           }
       }
    }

    static class ViewHolder{
        ImageView imageView;
        TextView textView;
    }

    class ImageAsyncTask extends AsyncTask<String, Void, Bitmap>{

        private DiskLruCache diskLruCache;
        private int index;
        private ListView listView;

        public ImageAsyncTask(ListView listView, DiskLruCache diskLruCache, int index) {
            this.listView = listView;
            this.diskLruCache  = diskLruCache;
            this.index = index;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            if (isCancelled()) return null;

            String url = strings[0];
            String key = Util.md5(url);
            DiskLruCache.Editor editor;
            DiskLruCache.Snapshot snapshot;
            OutputStream out;
            FileInputStream in;
            Bitmap bitmap = null;

            try {
                editor = diskLruCache.edit(key);
                out = editor.newOutputStream(0);
                if (Util.toStream(url, out)){
                    Log.d(TAG, "Success, Async save bitmap to cache");
                    editor.commit();
                }else{
                    Log.d(TAG, "Failed, Async save bitmap to cache");
                    editor.abort();
                }
                diskLruCache.flush();

                snapshot = diskLruCache.get(key);
                if (snapshot != null){
                    in = (FileInputStream)snapshot.getInputStream(0);
                    bitmap = Util.decodeSampleBitmapFormStream(in, reqWidth,reqHeight);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null){
                ImageView imageView = (ImageView)listView.findViewWithTag(index);
                if (imageView != null){
                    imageView.setImageBitmap(bitmap);
                }
            }

            tasks.remove(this);
        }
    }
}
