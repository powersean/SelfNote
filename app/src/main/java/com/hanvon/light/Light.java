package com.hanvon.light;

import android.os.SystemClock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <pre>
 * author : Sean
 * e-mail : seanpower@foxmail.com
 * time   : 2020/01/20
 * </pre>
 */
public class Light {

    private static ExecutorService lightExecutor = null;
    private static long startTime = 0L;
    private static volatile boolean isLightting = false;

    static {
        System.loadLibrary("HWLight");
    }

    /**
     * 打开红外补光灯
     */
    public static native void openInfrared();

    /**
     * 关闭红外补光灯
     */
    public static native void closeInfrared();

    /**
     * 控制可将光补光灯
     *
     * @param light 控制值范围1~10000  10000关闭 1亮度最大
     */
    public static native void setVisibleLight(int light);


    public static void close() {
        isLightting = false;
        Light.setVisibleLight(10000);
    }

    public static void open() {
        if (isLightting) {
            return;
        }
        isLightting = true;
        setVisibleLight(1);
        startTime = System.currentTimeMillis();
        if (lightExecutor == null) {
            lightExecutor = Executors.newSingleThreadExecutor();
        }

        lightExecutor.execute(new Runnable() {
            @Override
            public void run() {
                long endTime = 0;
                while (true) {
                    SystemClock.sleep(1000);
                    endTime = System.currentTimeMillis();
                    if (endTime - startTime >= 10000) {
                        close();
                        break;
                    }
                }
            }
        });
    }

    public static boolean isIsLightting() {
        return isLightting;
    }
}
