package com.razerdp.huobi.analysis.base.file;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import androidx.appcompat.app.AlertDialog;

import com.razerdp.huobi.analysis.base.AppContext;
import com.razerdp.huobi.analysis.utils.FileUtil;
import com.razerdp.huobi.analysis.utils.log.HLog;

import java.io.File;
import java.io.IOException;

/**
 * 读取该路径的时候需要授予读写权限
 * Created by 大灯泡 on 2019/4/18.
 */
public class AppFileHelper {
    private static final String TAG = "AppFileHelper";

    private static String storagePath;

    public static String ROOT_PATH = "razerdp/huobi/analysis/";
    public static final String DOWNLOAD_PATH = "download/";
    public static final String RESOURCE_PATH = "resource/";
    public static final String PIC_PATH = "image/";
    public static final String FILE_PATH = "files/";
    public static final String TEMP_PATH = "temp/";
    public static final String LOG_PATH = "log/";
    public static final String CACHE_PATH = "cache/";
    public static final String DB_PATH = "db/";

    public static void init() {
        storagePath = AppContext.getAppContext().getFilesDir().getAbsolutePath();
        storagePath = FileUtil.checkFileSeparator(storagePath);

        getAppRootPath();
        getImagePath();
        getFilePath();
        getTempPath();
        getDownloadPath();
        getLogPath();
        getCachePath();
        getDBPath();
        getResourcePath();
    }


    private static void checkAndMakeDir(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            boolean result = file.mkdirs();
            HLog.i(TAG, "新建文件：" + file.getAbsolutePath() + "  isLike：" + result);
        } else {
            HLog.i(TAG, "文件：" + file.getAbsolutePath() + "已经存在");
        }
    }

    public static String getSdCardPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    public static String getAppRootPath() {
        checkAndMakeDir(storagePath + ROOT_PATH);
        return storagePath + ROOT_PATH;
    }

    public static String getImagePath() {
        checkAndMakeDir(storagePath + ROOT_PATH + PIC_PATH);
        return storagePath + ROOT_PATH + PIC_PATH;
    }

    public static String getFilePath() {
        checkAndMakeDir(storagePath + ROOT_PATH + FILE_PATH);
        return storagePath + ROOT_PATH + FILE_PATH;
    }

    public static String getTempPath() {
        checkAndMakeDir(storagePath + ROOT_PATH + TEMP_PATH);
        return storagePath + ROOT_PATH + TEMP_PATH;
    }

    public static String getDownloadPath() {
        checkAndMakeDir(storagePath + ROOT_PATH + DOWNLOAD_PATH);
        return storagePath + ROOT_PATH + DOWNLOAD_PATH;
    }

    public static String getLogPath() {
        checkAndMakeDir(storagePath + ROOT_PATH + LOG_PATH);
        return storagePath + ROOT_PATH + LOG_PATH;
    }

    public static String getCachePath() {
        checkAndMakeDir(storagePath + ROOT_PATH + CACHE_PATH);
        return storagePath + ROOT_PATH + CACHE_PATH;
    }

    public static String getDBPath() {
        checkAndMakeDir(storagePath + (ROOT_PATH + DB_PATH));
        return storagePath + ROOT_PATH + DB_PATH;
    }

    public static String getResourcePath() {
        checkAndMakeDir(storagePath + (ROOT_PATH + RESOURCE_PATH));
        return storagePath + ROOT_PATH + RESOURCE_PATH;
    }


    public static String getInternalFilePath() {
        return AppContext.getAppContext().getFilesDir().getAbsolutePath();
    }


    private static void createNoMediaFile(String dir) {
        File file = new File(dir + ".nomedia");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean checkSDCard() {
        return Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
    }

    public static void showSDCardDisable(Context context, String title, String message, String ok) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(title).setMessage(message)
                .setPositiveButton(ok, null);
        AlertDialog dlg = builder.create();
        dlg.setCanceledOnTouchOutside(true);
        dlg.show();
    }

    public static void sendScanBroadcast(Context context, String path) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri uri = Uri.fromFile(new File(path));
        intent.setData(uri);
        context.sendBroadcast(intent);
    }

}
