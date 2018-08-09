/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.travis.listviewtest;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Junk drawer of utility methods. */
public final class Util {
  static final Charset US_ASCII = Charset.forName("US-ASCII");
  static final Charset UTF_8 = Charset.forName("UTF-8");
  private static final String TAG = "Util";

  private Util() {
  }

  static String readFully(Reader reader) throws IOException {
    try {
      StringWriter writer = new StringWriter();
      char[] buffer = new char[1024];
      int count;
      while ((count = reader.read(buffer)) != -1) {
        writer.write(buffer, 0, count);
      }
      return writer.toString();
    } finally {
      reader.close();
    }
  }

  /**
   * Deletes the contents of {@code dir}. Throws an IOException if any file
   * could not be deleted, or if {@code dir} is not a readable directory.
   */
  static void deleteContents(File dir) throws IOException {
    File[] files = dir.listFiles();
    if (files == null) {
      throw new IOException("not a readable directory: " + dir);
    }
    for (File file : files) {
      if (file.isDirectory()) {
        deleteContents(file);
      }
      if (!file.delete()) {
        throw new IOException("failed to delete file: " + file);
      }
    }
  }

  static void closeQuietly(/*Auto*/Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (RuntimeException rethrown) {
        throw rethrown;
      } catch (Exception ignored) {
      }
    }
  }

  /**
   * 获取磁盘缓存的存储路径
   * @param context
   * @param uniqueName
   * @return
   */
  public static File getDiskCacheDir(Context context, String uniqueName){
    String filePath = "";

    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
            || !Environment.isExternalStorageRemovable()){
      // SD 卡存在 或者不可移除
      try{
        filePath = context.getExternalCacheDir().getPath();
      }catch (Exception e ){
        Log.d(TAG, e.getMessage());
      }
    }else {
      filePath = context.getCacheDir().getPath();
    }

    Log.d(TAG, "file name is " + filePath + File.separator + uniqueName);
    return new File(filePath + File.separator + uniqueName);
  }


  /**
   * 获取app的版本号
   * @param context
   * @return
   */
  public static int getAppVersion(Context context){
    PackageInfo info;
    try{
      info = context.getPackageManager().getPackageInfo(
              context.getPackageName(),0);
      return info.versionCode;
    }catch (PackageManager.NameNotFoundException e ){
      Log.d(TAG, e.getMessage());
    }

    return 1;
  }

  public static int calculateInSampleSize(BitmapFactory.Options options, int requW, int reqH) {

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

  /**
   * InputStream 转为 byte[]
   * @param in
   * @return
   */
  public static byte[] inputStreamToByteArray(FileInputStream in) {

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int len;

    try {
      while((len = in.read(buffer)) != -1){
        outputStream.write(buffer, 0, len);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }finally {
      try {
        in.close();
        outputStream.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return outputStream.toByteArray();
  }

  /**
   * 压缩图片
   */
  public static Bitmap decodeSampleBitmapFormStream(FileInputStream in, int reqWidth, int reqHeight) {

    BitmapFactory.Options options = new BitmapFactory.Options();

    options.inJustDecodeBounds = true;
    byte[] data = Util.inputStreamToByteArray(in);
    Bitmap beforeBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
    options.inSampleSize = Util.calculateInSampleSize(options, reqWidth, reqHeight);
    options.inJustDecodeBounds = false;

    Bitmap afterBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);

    return afterBitmap;
  }

  /**
   * 将String转成MD5码，生成缓存文件的名字
   */
  public static String md5(String url) {

    MessageDigest md5 = null;
    try {
      md5 = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();
      return "";
    }

    char[] charArray = url.toCharArray();
    byte[] byteArray = new byte[charArray.length];

    for (int i = 0; i < charArray.length; i++){
      byteArray[i] = (byte)charArray[i];
    }

    byte[] md5Bytes = md5.digest(byteArray);

    StringBuffer hexValue = new StringBuffer();

    for (int i = 0; i < md5Bytes.length; i++){
      int val = ((int)md5Bytes[i]) & 0xff;
      if (val < 16){
        hexValue.append("0");
      }
      hexValue.append(Integer.toHexString(val));
    }

    return hexValue.toString();
  }

  /**
   * 将bitmap转为OutputStream
   */
  public static boolean toStream(String url, OutputStream out) {
    BitmapFactory.Options options = new BitmapFactory.Options();

    options.inJustDecodeBounds = true;
    Bitmap bitmap = BitmapFactory.decodeFile(url, options);
    options.inSampleSize = calculateInSampleSize(options, 100,100);
    options.inJustDecodeBounds = false;

    Bitmap afterBitmap = BitmapFactory.decodeFile(url, options);


    afterBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);

    if (out != null){
      return true;
    }else {
      return false;
    }
  }
}
