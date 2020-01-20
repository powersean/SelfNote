package com.org.sean.selfnote;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.lang.reflect.Method;

/**
 * <pre>
 * author : Sean
 * e-mail : seanpower@foxmail.com
 * time   : 2020/01/16
 * </pre>
 */
public class CameraActivity extends AppCompatActivity {
    private int camera_way;
    private int carema_option;
    private SurfaceView surfaceView1, surfaceView2;
    private SurfaceHolder surfaceHolder1, surfaceHolder2;
    private Camera camera1, camera2;
    private Camera.Parameters parameters;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        if (!parseData()) {
            finish();
        }
        parseSurface();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (camera1 != null) {
            camera1.release();
            camera1 = null;
        }
        if (camera2 != null) {
            camera2.release();
            camera2 = null;
        }
        if (surfaceView1 != null) {
            surfaceView1.destroyDrawingCache();
        }
        if (surfaceView2 != null) {
            surfaceView2.destroyDrawingCache();
        }
    }

    private boolean parseData() {
        Intent intent = getIntent();
        if (intent == null) {
            return false;
        }
        camera_way = intent.getIntExtra(MainActivity.CAMERA_WAY, 2);
        carema_option = intent.getIntExtra(MainActivity.CAMERA_OPTION, 0);
        return true;
    }


    private void parseSurface() {
        surfaceView1 = findViewById(R.id.surfaceview1);
        surfaceHolder1 = surfaceView1.getHolder();
        surfaceHolder1.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder1.addCallback(new surface1callback());

        surfaceView2 = findViewById(R.id.surfaceview2);
        surfaceHolder2 = surfaceView2.getHolder();
        surfaceHolder2.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceHolder2.addCallback(new surface2callback());
    }

    class surface1callback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            camera1 = openCamera(1, surfaceHolder);
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            camera1.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean b, Camera camera) {
                    if (b) {
                        initCamera(camera1);
                        camera.cancelAutoFocus();
                    }
                }
            });
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        }
    }

    class surface2callback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            camera2 = openCamera(0, surfaceHolder);
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            camera2.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean b, Camera camera) {
                    if (b) {
                        initCamera(camera2);
                        camera.cancelAutoFocus();
                    }
                }
            });
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        }
    }

    private void initCamera(Camera camera) {
        parameters = camera.getParameters();
        parameters.setPictureFormat(PixelFormat.JPEG);
//        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// 连续对焦
        setDisplay(parameters, camera);
        camera.setParameters(parameters);
        camera.startPreview();
        camera.cancelAutoFocus();// 如果要实现连续的自动对焦，这一句必须加上
    }

    private void setDisplay(Camera.Parameters parameters, Camera camera) {
        if (Integer.parseInt(Build.VERSION.SDK) >= 8) {
            setDisplayOrientation(camera, 0);
        } else {
            parameters.setRotation(90);
        }
    }

    // 实现的图像的正确显示
    private void setDisplayOrientation(Camera camera, int i) {
        Method downPolymorphic;
        try {
            downPolymorphic = camera.getClass().getMethod("setDisplayOrientation", new Class[]{int.class});
            if (downPolymorphic != null) {
                downPolymorphic.invoke(camera, new Object[]{i});
            }
        } catch (Exception e) {
            Toast.makeText(this, getResources().getString(R.string.image_error),
                    Toast.LENGTH_SHORT).show();
            Log.e("Came_e", getResources().getString(R.string.image_error));
        }
    }

    private Camera openCamera(int num, SurfaceHolder holder) {
//        int cameraNum = Camera.getNumberOfCameras();
//        if (cameraNum <= 0 || cameraNum < num + 1) {
//            Toast.makeText(this, getResources().getString(R.string.not_support),
//                    Toast.LENGTH_SHORT).show();
//            finish();
//        }
        detScope();
        Camera camera = Camera.open(num);
        try {
            // 设置预览监听
            camera.setPreviewDisplay(holder);
            Camera.Parameters parameters = camera.getParameters();

            if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                parameters.set("orientation", "portrait");
                camera.setDisplayOrientation(90);
                parameters.setRotation(90);
            } else {
                parameters.set("orientation", "landscape");
                camera.setDisplayOrientation(0);
                parameters.setRotation(0);
            }
            camera.setParameters(parameters);
            // 启动摄像头预览
            camera.startPreview();
            System.out.println("camera.start:" + String.valueOf(num));
            return camera;
        } catch (IOException e) {
            e.printStackTrace();
            camera.release();
            Toast.makeText(this, getResources().getString(R.string.init_error)
                    + String.valueOf(num), Toast.LENGTH_SHORT).show();
            System.out.println("camera.error");
            return null;
        }
    }

    private void detScope() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            System.out.println("ok");
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
    }
}
