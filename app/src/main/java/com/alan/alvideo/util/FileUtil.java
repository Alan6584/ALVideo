package com.alan.alvideo.util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;

public class FileUtil {

    public static final String AL_DIR = "ALVideo";

    /**
     * 获取外部存储空间路径，如果获取不到则返回缓存路径
     * @param context
     * @return
     */
    public static String getExternalDirectory(Context context){
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return Environment.getExternalStorageDirectory().getPath();
        }else {
            return getCacheDirectory(context, false).getAbsolutePath();
        }
    }

    /**
     * 获取外部cache存储目录
     * @param context
     * @return
     */
    private static File getExternalCacheDirectory(Context context) {
        File cacheDir = context.getExternalCacheDir();
        if (cacheDir != null && !cacheDir.exists()) {
            if (!cacheDir.mkdirs()) {
                Log.d(FileUtil.class.getName(), "could not create SDCard cache");
                return null;
            }
        }
        return cacheDir;
    }

    /**
     * 获取cache目录
     * @param context
     * @param preferExternal
     * @return
     */
    public static File getCacheDirectory(Context context, boolean preferExternal) {
        File appCacheDir = null;

        if (preferExternal && Environment.MEDIA_MOUNTED.equals(
                Environment.getExternalStorageState())) {
            appCacheDir = getExternalCacheDirectory(context);
        }

        if (appCacheDir == null) {
            appCacheDir = context.getCacheDir();
        }

        if (appCacheDir == null) {
            String cacheDirPath = "/data/data/" + context.getPackageName() + "/cache/";
            Log.d(FileUtil.class.getName(),
                    "Can't define system cache directory! use " + cacheDirPath);
            appCacheDir = new File(cacheDirPath);
        }

        return appCacheDir;
    }

}
