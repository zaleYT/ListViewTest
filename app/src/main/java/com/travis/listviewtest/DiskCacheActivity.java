package com.travis.listviewtest;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageInfo;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by yutao on 2018/8/4.
 */

public class DiskCacheActivity extends Activity implements AbsListView.OnScrollListener {

    private static final String TAG = "DiskCacheActivity";

    public ListView listView = null;
    public ListViewAdapterWithCache adapter = null;
    public int start_index;
    public int end_index;
    public List<String> list;
    //public List<String> md5s;
    public boolean isInit = true;
    public ImageView imageView;

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
        setContentView(R.layout.activity_main);


        if (Build.VERSION.SDK_INT > 23) {
            getRuntimePermission();
        }else {
            init();
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

    private void init(){

        initImagePath();

        if (list.size() == 0){
            setContentView(R.layout.empty_screen);
            return;
        }

        File directory = Util.getDiskCacheDir(this, "bitmap");
        if (!directory.exists()){
            directory.mkdirs();
        }

        int appVersion = Util.getAppVersion(this);
        DiskLruCache diskLruCache = null;
        try {
            // 参数1 表示可一个key对应一个缓存文件，
            // 1024*1024*100 表示缓存大小为100M
            diskLruCache = DiskLruCache.open(directory, appVersion,
                    1, 1024*1024*100);
        } catch (IOException e) {
            e.printStackTrace();
        }

        listView = (ListView) findViewById(R.id.my_listview);
        //LayoutInflater inflater = LayoutInflater.from(this);
        adapter  = new ListViewAdapterWithCache(this, list,
                diskLruCache, listView);
        listView.setOnScrollListener(this);
        listView.setAdapter(adapter);
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
                Log.d(TAG, "image, path:" + cursor.getString(INDEX_IMAGE_PATH));
            }
        }
        cursor.close();
    }

    @Override
    public void onScrollStateChanged(AbsListView absListView, int scrollState) {

        if (scrollState == SCROLL_STATE_IDLE){// 滑动停止的时候，加载图片
            String url = "";
            String key = "";

            for (int i = start_index; i < end_index; i ++){
                url = list.get(i);
                key = Util.md5(url);
                adapter.loadImage(url, key, i);
            }
        }else {
            //  滑动的时候取消任务
            adapter.cancleTask();
        }
    }

    @Override
    public void onScroll(AbsListView absListView, int firstVisibleItem,
                         int visibleItemCount, int totalItemCount) {

        start_index = firstVisibleItem;
        end_index = start_index + visibleItemCount;
        if (isInit == true && visibleItemCount > 0){
            String url = "";
            String key = "";

            for (int i = start_index; i < end_index; i++){
                url = list.get(i);
                key = Util.md5(url);
                adapter.loadImage(url,key, i);
            }

            isInit = false;
        }
    }
}


