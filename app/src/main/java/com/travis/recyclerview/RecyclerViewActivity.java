package com.travis.recyclerview;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.travis.listviewtest.DiskLruCache;
import com.travis.listviewtest.R;
import com.travis.listviewtest.Util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yutao on 2018/8/9.
 *
 *  参考博客 https://www.cnblogs.com/anni-qianqian/p/6587329.html
 */

public class RecyclerViewActivity extends Activity {

    public static final String TAG = "RecyclerViewActivity";

    private RecyclerView mRecyclerView;

    private boolean isInit = false;
    private RecyclerAdpater mRecyclerAdapter;

    private List<String> list = new ArrayList<String>();

    public static final int INDEX_IMAGE_ID = 0;
    public static final int INDEX_IMAGE_PATH = 1;
    public static final int INDEX_IMAGE_SIZE = 2;
    public static final int INDEX_IMAGE_DISPLAY_NAME = 3;

    public static final int EXTERNAL_STORAGE_REQ_CODE = 15 ;

    String[] projImage = {
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA, // 路径
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DISPLAY_NAME
    };

    Uri mImangeUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recycler_view_layout);

        mRecyclerView = (RecyclerView)findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        if (Build.VERSION.SDK_INT > 23){
            getRuntimePermission();
        }else {
            init();
        }
    }

    private void init(){
        initImagePath();

        DiskLruCache diskLruCache = null;

        File dir = Util.getDiskCacheDir(this, "bitmap");
        if (!dir.exists()){
            dir.mkdir();
        }

        try {
            diskLruCache = DiskLruCache.open(dir,
                    Util.getAppVersion(this), 1, 100 * 1024 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mRecyclerAdapter = new RecyclerAdpater(this, list, mRecyclerView, diskLruCache);
        mRecyclerView.setAdapter(mRecyclerAdapter);
        mRecyclerView.addOnScrollListener(new RecycleScrollListener());
    }


    private void initImagePath() {
        list = new ArrayList<String>();

        final Cursor cursor = getContentResolver().query(
                mImangeUri,
                projImage,
                null,
                null,
                null
        );

        if (cursor != null){
            String path;
            while (cursor.moveToNext()){
                path = cursor.getString(INDEX_IMAGE_PATH);
                list.add(path);
                //Log.d(TAG, "image, path:" + cursor.getString(INDEX_IMAGE_PATH));
            }
        }

        cursor.close();
    }

    /**
     * recycleview 的滚动监听
     */
    private class RecycleScrollListener extends RecyclerView.OnScrollListener{

        int firstVisibleIndex;
        int lastVisibleIndex;

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            //super.onScrollStateChanged(recyclerView, newState);

            if (newState == RecyclerView.SCROLL_STATE_IDLE){
                for (int i = firstVisibleIndex; i <= lastVisibleIndex; i++){
                    String url = list.get(i);
                    String md5 = Util.md5(url);

                    mRecyclerAdapter.loadImage(url, md5, i);
                }
            }else {
                mRecyclerAdapter.cancleTask();
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            //super.onScrolled(recyclerView, dx, dy);

            RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
            if (manager instanceof  GridLayoutManager){

                //int first1 =((GridLayoutManager)manager).findFirstCompletelyVisibleItemPosition();
                //int last1 = ((GridLayoutManager)manager).findLastCompletelyVisibleItemPosition();
                firstVisibleIndex = ((GridLayoutManager)manager).findFirstVisibleItemPosition();
                lastVisibleIndex = ((GridLayoutManager)manager).findLastVisibleItemPosition();
                int total = ((GridLayoutManager)manager).getItemCount();

                Log.d(TAG, "total:" +total);

                if (total > 0 && !isInit){

                    for (int i = firstVisibleIndex; i <= lastVisibleIndex; i++){
                        String url = list.get(i);
                        String md5 = Util.md5(url);

                        mRecyclerAdapter.loadImage(url, md5, i);
                    }

                    isInit = true;
                }
            }
        }
    }

    private void getRuntimePermission() {
        // 没有获得权限
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            // 如果APP的权限曾经被用户拒绝过，就需要在这里更用户做出解释
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)){
                //Toast.makeText(this, "please give me the permission", Toast.LENGTH_LONG).show();
            }
            // 请求权限
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    EXTERNAL_STORAGE_REQ_CODE);
        }else {
            // 获得了权限
            init();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == EXTERNAL_STORAGE_REQ_CODE){
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                init();
            }else{
                finish();
            }
        }
    }
}
