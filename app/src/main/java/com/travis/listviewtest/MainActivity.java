package com.travis.listviewtest;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "main";

    private int mfirstVisibleitem;
    private int mVisibleItemCount;
    private int mTotalItemCount;
    private int mScrollStates;

    ListView mList;
    MyListAdapter mListAdapter;

    List<String> itemTexts = new ArrayList<String>();
    List<String> imagePaths = new ArrayList<String>();

    private MyHandlerThread mHandlerThread;
    private Handler mHandler;

    public static final int INDEX_IMAGE_ID = 0;
    public static final int INDEX_IMAGE_PATH = 1;
    public static final int INDEX_IMAGE_SIZE = 2;
    public static final int INDEX_IMAGE_DISPLAY_NAME = 3;

    public static final int EXTERNAL_STORAGE_REQ_CODE = 15 ;

    int LruCacheSize;
    private LruCache<String, Bitmap> mImageCache;

    String[] projImage = {
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA, // 路径
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DISPLAY_NAME
    };

    Uri mImangeUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mList = (ListView)findViewById(R.id.my_listview);
        mList.setOnScrollListener(new onScrollListnerImpl());
        mListAdapter = new MyListAdapter(this);
        mHandlerThread = new MyHandlerThread("loadImage");

        if (Build.VERSION.SDK_INT > 23) {
            getRuntimePermission();
        }

        // 内存缓存
        LruCacheSize = (int)Runtime.getRuntime().maxMemory() / 8;
        mImageCache = new LruCache<String, Bitmap>(LruCacheSize){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount(); // 返回每一张图片的大小
            }
        };

        initImagePath();
    }

    private void initImagePath() {
        final Cursor cursor = getContentResolver().query(
                mImangeUri,
                projImage,
                null,
                null,
                null
        );
        if (cursor != null){
            int i = 100; // 测试用，打印出100张
            String path;
            while (cursor.moveToNext()){
                path = cursor.getString(INDEX_IMAGE_PATH);
                imagePaths.add(path);
                Log.d(TAG, "image, path:" + cursor.getString(INDEX_IMAGE_PATH));
                //i--;
            }
        }
        cursor.close();
    }


    @Override
    protected void onResume() {
        super.onResume();
        for (int i=1; i <= 100; i++){
            itemTexts.add("Text" + i);
        }
        mList.setAdapter(mListAdapter);

    }

    private void getRuntimePermission() {
        // 没有获得权限
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            // 如果APP的权限曾经被用户拒绝过，就需要在这里更用户做出解释
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)){
                Toast.makeText(this, "please give me the permission", Toast.LENGTH_SHORT).show();
            }
            // 请求权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    EXTERNAL_STORAGE_REQ_CODE);
        }else {
            // 获得了权限
            /*if (mHandlerThread.getState() == Thread.State.NEW
                    || mHandlerThread.getState() == Thread.State.TERMINATED) {
                mHandlerThread.start();
            }*/
            //new MyAsyncTask().execute();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                       @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case EXTERNAL_STORAGE_REQ_CODE:
                // 如果请求被拒绝，那么通常grantResults数组为空
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    // 申请成功，进行下面的操作
                    //mHandlerThread.start();
                    //new MyAsyncTask().execute();
                }else {
                    //申请失败
                }
                break;

        }
    }


    public class onScrollListnerImpl implements AbsListView.OnScrollListener{

        @Override
        public void onScrollStateChanged(AbsListView absListView, int state) {
            mScrollStates = state;
        }

        @Override
        public void onScroll(AbsListView absListView, int firstVisibleitem,
                             int visibleItmeCount, int totalItemCount) {
            mfirstVisibleitem = firstVisibleitem;
            mVisibleItemCount = visibleItmeCount;
            mTotalItemCount = totalItemCount;

        }
    }

    private class MyHandler extends Handler{
        //public MyHandler(Looper looper) {
        //    super(looper);
        //}

        @Override
        public void handleMessage(Message msg) {
            mListAdapter.notifyDataSetChanged();
        }
    }

    public class MyListAdapter extends BaseAdapter{

        Context context;
        LayoutInflater inflater;

        public MyListAdapter(Context context) {
            this.context = context;
            inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return itemTexts.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View contentView, ViewGroup viewGroup) {

            ViewHolder viewHolder;

            if (contentView == null){
                viewHolder = new ViewHolder();
                contentView = inflater.inflate(R.layout.list_item_layout, null);
                viewHolder.image = (ImageView)contentView.findViewById(R.id.item_image);
                viewHolder.textView = (TextView)contentView.findViewById(R.id.item_text);
                contentView.setTag(viewHolder);
            }else {
                viewHolder = (ViewHolder)contentView.getTag();
            }

            viewHolder.image.setTag(imagePaths.get(i));
            if (mImageCache.get(imagePaths.get(i)) != null){
                viewHolder.image.setImageBitmap(mImageCache.get(imagePaths.get(i)));
            }else {
                // 缓存中没有对应的图片，则由task导入
                MyAsyncTask it = new MyAsyncTask();
                it.execute(imagePaths.get(i));
            }

            viewHolder.textView.setText(itemTexts.get(i));

            return contentView;
        }
    }

    public class ViewHolder{
        ImageView image;
        TextView textView;
    }

    public class MyAsyncTask extends AsyncTask<String, Void, Bitmap>{

        private String imageUrl;

        @Override
        protected Bitmap doInBackground(String... params) {

            Bitmap beforeBitmap = null;
            Bitmap afterBitmap = null;

            imageUrl = params[0];

            BitmapFactory.Options options = new BitmapFactory.Options();
            // 压缩图片
            options.inJustDecodeBounds = true;// true 表示禁止为bitmap分配内存
            beforeBitmap = BitmapFactory.decodeFile(imageUrl,options);
            options.inSampleSize = calculateInSampleSize(options, 100,100);
            options.inJustDecodeBounds = false;
            // 重新获取图片
            afterBitmap = BitmapFactory.decodeFile(imageUrl, options);

            if (mImageCache.get(imageUrl) == null){
                mImageCache.put(imageUrl, afterBitmap);
            }

            return afterBitmap;
        }

        private int calculateInSampleSize(BitmapFactory.Options options, int requW, int reqH) {

            int width = options.outWidth;
            int height = options.outHeight;

            int inSampleSize = 1; // 初始值是没有压缩的

            if (width > requW || height > reqH){
                int widthRation = Math.round((float)width/requW);
                int heightRation = Math.round((float)height/reqH);
                inSampleSize = widthRation < heightRation ? widthRation :heightRation;
            }

            System.out.print("压缩比：" + inSampleSize);
            return inSampleSize;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            //super.onPostExecute(aBoolean);
            ImageView iv = (ImageView)mList.findViewWithTag(imageUrl);
            if (iv != null && result != null){
                iv.setImageBitmap(result);
            }
        }
    }

    public class MyHandlerThread extends HandlerThread {

        public MyHandlerThread(String name) {
            super(name);

        }

        @Override
        public void run() {
            super.run();
            final Cursor cursor = getContentResolver().query(
                    mImangeUri,
                    projImage,
                    null,
                    null,
                    null
            );
            if (cursor != null){
                int i = 100; // 测试用，打印出100张
                while (cursor.moveToNext() && i > 0){
                    imagePaths.add(cursor.getString(INDEX_IMAGE_PATH));
                    Log.d(TAG, "image, path:" + cursor.getString(INDEX_IMAGE_PATH));
                    i--;
                }
            }
            cursor.close();
            mHandler = new MyHandler();
            mHandler.sendEmptyMessage(1);
        }
    }
}
