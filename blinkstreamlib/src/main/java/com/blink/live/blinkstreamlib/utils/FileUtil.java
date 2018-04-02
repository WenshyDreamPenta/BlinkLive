package com.blink.live.blinkstreamlib.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

/**
 * <pre>
 *     author : wangmingxing
 *     time   : 2018/3/27
 *     desc   : 文件操作类
 * </pre>
 */
public class FileUtil {
    private static final String TAG = "FileUtil";
    private static final SimpleDateFormat mDateTimeFormat = new SimpleDateFormat("yyyy-MM-DD-hh-mm-ss", Locale.US);

    /**
     * 获取缓存root路径
     *
     * @param context 上下文
     * @param isExternFirst 是否外存优先
     * @return file
     */
    public static File getStorageRoot(Context context, String dirName, boolean isExternFirst) {
        File cacheDir = null;
        if ((
                Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                        !Environment.isExternalStorageRemovable()) && isExternFirst) {
            cacheDir = context.getExternalCacheDir();
        }
        else {
            cacheDir = context.getCacheDir();
        }
        File dir = new File(cacheDir, dirName);
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * generate output file
     *
     * @param type Environment.DIRECTORY_MOVIES / Environment.DIRECTORY_DCIM etc.
     * @param ext  .mp4(.m4a for audio) or .png
     * @return return null when this app has no writing permission to external storage.
     */
    public static  File getCaptureFile(final String type, final String ext, String dirName) {
        final File dir = new File(Environment.getExternalStoragePublicDirectory(type), dirName);
        LogUtil.d(TAG, "path = " + dir.toString());
        dir.mkdirs();
        if (dir.canWrite()) {
            return new File(dir, getDateTimeString() + ext);
        }
        return null;
    }
    /**
     * 新建tmp目录,tmp/xxx/
     *
     * @param dirName 文件夹名
     * @return String
     */
    public String newTmpDir(Context context,String dirName, String rootName, String tempDir) {
        File tmpDir = new File(FileUtil.getStorageRoot(context, rootName, true), tempDir);
        if (!tmpDir.exists() || !tmpDir.isDirectory()) {
            tmpDir.mkdirs();
        }
        File dir = new File(tmpDir, dirName);
        if (!dir.exists() || !dir.isDirectory()) {
            dir.mkdirs();
        }
        return dir.getAbsolutePath() + FileUtil.getDateTimeString() + ".mp4";
    }

    /**
     * get current date and time as String
     *
     * @return date String
     */
    public static String getDateTimeString() {
        final GregorianCalendar now = new GregorianCalendar();
        return mDateTimeFormat.format(now.getTime());
    }
}
