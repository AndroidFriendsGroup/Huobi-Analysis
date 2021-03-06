package com.razerdp.huobi.analysis.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Log;

import com.razerdp.huobi.analysis.base.AppContext;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import razerdp.util.log.PopupLog;

public class FileUtil {

    private static final int IO_BUFFER_SIZE = 1024;

    public static boolean hasSDCard() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    private void writeFile(String fileName, String writeStr) {
        try {

            FileOutputStream fout = new FileOutputStream(fileName);
            byte[] bytes = writeStr.getBytes();

            fout.write(bytes);
            fout.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String readFile(String filePath) {
        String result = "";
        try {
            FileInputStream fileInputStream = new FileInputStream(filePath);
            int length = fileInputStream.available();
            byte[] buffered = new byte[length];
            fileInputStream.read(buffered);

            result = new String(buffered, Charset.defaultCharset());

            fileInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;

    }

    public static String getFileName(String path) {
        if (TextUtils.isEmpty(path)) return null;
        int start = path.lastIndexOf("/");
        if (start != -1) {
            return path.substring(start + 1);
        } else {
            return null;
        }
    }

    public static String getFileSuffix(String path) {
        if (TextUtils.isEmpty(path)) return null;
        int start = path.lastIndexOf(".");
        if (start != -1) {
            return path.substring(start + 1);
        } else {
            return null;
        }

    }

    /**
     * ????????????????????????"/"????????????????????????"/"???????????????????????????????????????
     *
     * @param path
     * @return
     */
    public static String checkFileSeparator(String path) {
        if (path != null && path.length() != 0) {
            if (!path.endsWith(File.separator)) {
                return path.concat(File.separator);
            } else {
                final int sourceStringLength = path.length();
                int index = sourceStringLength - 1;
                while (path.charAt(index) == File.separatorChar) {
                    index--;
                }
                path = path.substring(0, index + 1);
                return path.concat(File.separator);
            }
        }
        return path;
    }

    /**
     * ??????????????????????????????
     */
    public static boolean isFileCanReadAndWrite(String filePath) {
        if (null != filePath && filePath.length() > 0) {
            File f = new File(filePath);
            if (null != f && f.exists()) {
                return f.canRead() && f.canWrite();
            }
        }
        return false;
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] b = new byte[IO_BUFFER_SIZE];
        int read;
        while ((read = in.read(b)) != -1) {
            out.write(b, 0, read);
        }
    }

    /**
     * ??????????????????
     *
     * @param oldPath String ???????????????
     * @param newPath String ???????????????
     * @return boolean
     */
    public static boolean copyFile(String oldPath, String newPath) {
        try {
            int byteread = 0;
            File oldfile = new File(oldPath);
            if (oldfile.exists()) { // ???????????????
                InputStream inStream = new FileInputStream(oldPath); // ???????????????
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[IO_BUFFER_SIZE];

                while ((byteread = inStream.read(buffer)) != -1) {
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
                fs.close();
            }
        } catch (Exception e) {
            PopupLog.e(e);
            return false;
        }
        return true;
    }

    /**
     * ????????????
     *
     * @param strFileName ?????????
     * @param ins         ???
     */
    public static void writeToFile(String strFileName, InputStream ins) {
        try {
            File file = new File(strFileName);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

            FileOutputStream fouts = new FileOutputStream(file);
            int len;
            int maxSize = 1024 * 1024;
            byte buf[] = new byte[maxSize];
            while ((len = ins.read(buf, 0, maxSize)) != -1) {
                fouts.write(buf, 0, len);
                fouts.flush();
            }

            fouts.close();
        } catch (IOException e) {
            PopupLog.e(e);
        }
    }

    /**
     * ????????????
     *
     * @param strFileName ?????????
     * @param bytes       bytes
     */
    public static boolean writeToFile(String strFileName, byte[] bytes) {
        try {
            File file = new File(strFileName);

            FileOutputStream fouts = new FileOutputStream(file);
            fouts.write(bytes, 0, bytes.length);
            fouts.flush();
            fouts.close();
            return true;
        } catch (IOException e) {
            PopupLog.e(e);
        }
        return false;
    }

    /**
     * Prints some data to a file using a BufferedWriter
     */
    public static boolean writeToFile(String filename, String data) {
        BufferedWriter bufferedWriter = null;
        try {
            // Construct the BufferedWriter object
            bufferedWriter = new BufferedWriter(new FileWriter(filename));
            // Start writing to the output stream
            bufferedWriter.write(data);
            return true;
        } catch (FileNotFoundException e) {
            PopupLog.e(e);
        } catch (IOException e) {
            PopupLog.e(e);
        } finally {
            // Close the BufferedWriter
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.flush();
                    bufferedWriter.close();
                }
            } catch (IOException e) {
                PopupLog.e(e);
            }
        }
        return false;
    }


    public static void Write(String fileName, String message) {

        try {
            FileOutputStream outSTr = null;
            try {
                outSTr = new FileOutputStream(new File(fileName));
            } catch (FileNotFoundException e) {
                PopupLog.e(e);
            }
            BufferedOutputStream Buff = new BufferedOutputStream(outSTr);
            byte[] bs = message.getBytes();
            Buff.write(bs);
            Buff.flush();
            Buff.close();
        } catch (MalformedURLException e) {
            PopupLog.e(e);
        } catch (IOException e) {
            PopupLog.e(e);
        }
    }

    public static void Write(String fileName, String message, boolean append) {
        try {
            FileOutputStream outSTr = null;
            try {
                outSTr = new FileOutputStream(new File(fileName), append);
            } catch (FileNotFoundException e) {
                PopupLog.e(e);
            }
            BufferedOutputStream Buff = new BufferedOutputStream(outSTr);
            byte[] bs = message.getBytes();
            Buff.write(bs);
            Buff.flush();
            Buff.close();
        } catch (MalformedURLException e) {
            PopupLog.e(e);
        } catch (IOException e) {
            PopupLog.e(e);
        }
    }

    /**
     * ???????????? ????????????????????????????????????
     *
     * @param path String ??????
     */
    public static void deleteFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        if (!file.isDirectory()) {// ?????????????????????????????????
            file.delete();
            return;
        }
        File files[] = file.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                deleteFile(files[i].getAbsolutePath());// ?????????????????????????????????
            }
            files[i].delete();
        }
        file.delete();
    }

    /**
     * ???????????? ????????????????????????????????????(????????????deleteFile(String path)?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????EBUSY (Device or resource busy)?????????)
     *
     * @param path String ??????
     */
    public static void deleteFileSafely(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        if (!file.isDirectory()) {// ?????????????????????????????????
            safelyDelete(file);
            return;
        }
        File files[] = file.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                deleteFileSafely(files[i].getAbsolutePath());// ?????????????????????????????????
            }
            safelyDelete(files[i]);
        }
        safelyDelete(file);
    }

    /**
     * ????????????????????????????????????EBUSY (Device or resource busy)????????????
     */
    public static void safelyDelete(File file) {
        if (file == null || !file.exists()) return;
        try {
            final File to = new File(file.getAbsolutePath() + System.currentTimeMillis());
            file.renameTo(to);
            to.delete();
        } catch (Exception e) {
            PopupLog.e(e);
        }
    }

    /**
     * ????????????
     *
     * @throws Exception
     */
    public static long getFileSize(File file) {
        long size = 0;
        try {
            if (!file.exists()) {
                return size;
            }
            if (!file.isDirectory()) {
                size = file.length();
            } else {
                File[] fileList = file.listFiles();
                for (int i = 0; i < fileList.length; i++) {
                    if (fileList[i].isDirectory()) {
                        size = size + getFileSize(fileList[i]);
                    } else {
                        size = size + fileList[i].length();
                    }
                }
            }

        } catch (Exception e) {
            PopupLog.e(e);
        }
        return size;
    }

    /**
     * @return ???????????????????????????(MB???KB???)
     */
    public static String getFileLength(String filePath) {
        try {
            File file = new File(filePath);
            return fileLengthFormat(getFileSize(file));
        } catch (Exception e) {
            PopupLog.e(e);
            return "";
        }
    }

    /**
     * @return ???????????????????????????(MB???KB???)
     */
    public static String fileLengthFormat(long length) {
        String lenStr = "";
        DecimalFormat formater = new DecimalFormat("#0.##");
        if (length > 0 && length < 1024) {
            lenStr = formater.format(length) + " Byte";
        } else if (length < 1024 * 1024) {
            lenStr = formater.format(length / 1024.0f) + " KB";
        } else if (length < 1024 * 1024 * 1024) {
            lenStr = formater.format(length / (1024 * 1024.0f)) + " MB";
        } else {
            lenStr = formater.format(length / (1024 * 1024 * 1024.0f)) + " GB";
        }
        return lenStr;
    }

    /**
     * ???????????????????????? ????????????????????????????????????????????????.?????????????????????
     *
     * @param fileName ?????????
     * @return ???????????????????????????
     */
    public static String pathExtension(String fileName) {
        int point = fileName.lastIndexOf('.');
        int length = fileName.length();
        if (point == -1 || point == length - 1) {
            return "";
        } else {
            return fileName.substring(point, length);
        }
    }

    /**
     * ????????????????????????????????????MIME?????????
     */
    @SuppressLint("DefaultLocale")
    public static String getMIMEType(File file) {

        String type = "*/*";
        String fName = file.getName();
        // ??????????????????????????????"."???fName???????????????
        int dotIndex = fName.lastIndexOf(".");
        if (dotIndex < 0) {
            return type;
        }
        /* ???????????????????????? */
        String end = fName.substring(dotIndex, fName.length()).toLowerCase();
        if (end == "") {
            return type;
        }
        // ???MIME?????????????????????????????????????????????MIME?????????
        for (int i = 0; i < MIME_MapTable.length; i++) {
            if (end.equals(MIME_MapTable[i][0])) {
                type = MIME_MapTable[i][1];
            }
        }
        return type;
    }

    public static void moveFile(String oldPath, String newPath) {
        copyFile(oldPath, newPath);
        delFile(oldPath);
    }

    public static void delFile(String filePathAndName) {
        try {
            File myDelFile = new File(filePathAndName);
            myDelFile.delete();
        } catch (Exception e) {
            System.out.println("????????????????????????");
            e.printStackTrace();
        }
    }

    private static final String[][] MIME_MapTable = {
            // {???????????? MIME??????}
            {".doc", "application/msword"}, {".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"},
            {".xls", "application/vnd.ms-excel"}, {".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"},
            {".pdf", "application/pdf"}, {".ppt", "application/vnd.ms-powerpoint"},
            {".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"}, {".txt", "text/plain"},
            {".wps", "application/vnd.ms-works"}, {"", "*/*"}
    };

    /**
     * ????????????
     */
    public static boolean Unzip(String zipFile, String targetDir) {
        try {
            int BUFFER = 4096; // ???????????????????????????4KB???
            String strEntry; // ????????????zip???????????????

            BufferedOutputStream dest = null; // ???????????????
            FileInputStream fis = new FileInputStream(zipFile);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry entry; // ??????zip???????????????

            while ((entry = zis.getNextEntry()) != null) {

                int count;
                byte data[] = new byte[BUFFER];
                strEntry = entry.getName();

                File entryFile = new File(targetDir + strEntry);
                File entryDir = new File(entryFile.getParent());
                if (!entryDir.exists()) {
                    entryDir.mkdirs();
                }

                FileOutputStream fos = new FileOutputStream(entryFile);
                dest = new BufferedOutputStream(fos, BUFFER);
                while ((count = zis.read(data, 0, BUFFER)) != -1) {
                    dest.write(data, 0, count);
                }
                dest.flush();
                dest.close();
            }
            zis.close();
            return true;
        } catch (IOException e) {
            return true;
        }
    }

    public void listMemoFolder() {

    }

    /**
     * ????????????????????????????????????
     */
    public static void deleteFolderFile(String filePath, boolean deleteThisPath) {
        if (!TextUtils.isEmpty(filePath)) {
            File file = new File(filePath);

            if (file.isDirectory()) {
                File files[] = file.listFiles();
                for (int i = 0; i < files.length; i++) {
                    deleteFolderFile(files[i].getAbsolutePath(), true);
                }
            }
            if (deleteThisPath) {
                if (!file.isDirectory()) {
                    file.delete();
                } else {
                    if (file.listFiles().length == 0) {
                        file.delete();
                    }
                }
            }
        }
    }

    public static String getFromAssets(String fileName) {
        String result = "";
        try {
            InputStream in = AppContext.getResources().getAssets().open(fileName);
            byte[] buffered = new byte[in.available()];
            in.read(buffered);
            result = new String(buffered, StandardCharsets.UTF_8);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static InputStream getAssetsInputStream(String fileName) {
        try {
            return AppContext.getAppContext().getResources().getAssets().open(fileName);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ?????????????????????SD????????????
     *
     * @param mContext
     * @param is_removale true=??????SD???
     * @return
     */
    public static String getStoragePath(Context mContext, boolean is_removale) {
        StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz = null;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getUrl");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(mStorageManager);
            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement);
                boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
                if (is_removale == removable) {
                    return path;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean createOrExistsDir(final File file) {
        return file != null && (file.exists() ? file.isDirectory() : file.mkdirs());
    }

    public static boolean exists(final File file) {
        return file != null && file.exists();
    }

    public static boolean createFile(final File file, boolean deleteOld) {
        if (file == null) return false;
        if (deleteOld && file.exists() && !file.delete()) return false;
        if (!createOrExistsDir(file.getParentFile())) return false;
        try {
            return file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static File createFile(File zipdir, File zipfile) {
        if (!zipdir.exists()) {
            boolean result = zipdir.mkdirs();
            PopupLog.d("TAG", "zipdir.mkdirs() = " + result);
        }
        if (!zipfile.exists()) {
            try {
                boolean result = zipfile.createNewFile();
                PopupLog.d("zipdir.createNewFile() = " + result);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("TAG", e.getMessage());
            }
        }
        return zipfile;
    }

    /**
     * ???????????????????????????????????????????????????????????????
     *
     * @param dir ???????????????????????????
     * @return ??????????????????true???????????????false
     */
    public static boolean deleteDir(File dir) {
        if (dir == null || !dir.exists()) return true;
        if (dir.isDirectory()) {
            String[] children = dir.list();
            // ????????????????????????????????????
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
        }
        // ?????????????????????????????????
        return dir.delete();
    }

    /**
     * ????????????????????????
     *
     * @param directory ??????????????????????????????
     * @return ??????????????????????????????byte
     */
    public static long folderSize(File directory) {
        long length = 0;
        for (File file : directory.listFiles()) {
            if (file.isFile())
                length += file.length();
            else
                length += folderSize(file);
        }
        return length;
    }


    public static int getInputStreamLength(InputStream in) throws IOException {
        int ch1 = in.read();
        int ch2 = in.read();
        int ch3 = in.read();
        int ch4 = in.read();
        if ((ch1 | ch2 | ch3 | ch4) < 0)
            throw new EOFException();
        return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
    }
}
