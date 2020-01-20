package com.org.sean.selfnote.util;

import android.os.Environment;

/**
 * <pre>
 * author : Sean
 * e-mail : seanpower@foxmail.com
 * time   : 2020/01/20
 * </pre>
 */
public class PathUtil {
    private static final String filePath = Environment.getExternalStorageDirectory() + "/MyCamera/";

    public static String getStoragePath(int cameraId) {
        if (0 == cameraId) {
            return filePath.concat("back/");
        } else {
            return filePath.concat("font/");
        }
    }
}
