package com.travis.recyclerview;

import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.travis.listviewtest.R;

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

        initImagePath();

        RecyclerAdpater adpater = new RecyclerAdpater(this, list);
        mRecyclerView.setAdapter(adpater);
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
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
            if (manager instanceof  GridLayoutManager){

                int first1 =((GridLayoutManager)manager).findFirstCompletelyVisibleItemPosition();
                int first2 = ((GridLayoutManager)manager).findFirstVisibleItemPosition();
                int last1 = ((GridLayoutManager)manager).findLastCompletelyVisibleItemPosition();
                int last2 = ((GridLayoutManager)manager).findLastVisibleItemPosition();

                Log.d(TAG, String.format("first1= %s, first2= %s, last1= %s, last2= %s", first1,first2,last1,last2));
            }
        }
    }
}
