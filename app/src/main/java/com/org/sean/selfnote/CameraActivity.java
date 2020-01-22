package com.org.sean.selfnote;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.hanvon.cameralight.CameraDetectManager;
import com.hanvon.cameralight.CommonListener;
import com.hanvon.cameralight.FaceDetectListener;
import com.hanvon.light.Light;
import com.org.sean.selfnote.util.BitmapUtil;
import com.org.sean.selfnote.util.PermisstionUtil;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * author : Sean
 * e-mail : seanpower@foxmail.com
 * time   : 2020/01/16
 * </pre>
 */
public class CameraActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String FILEPATH = Environment.getExternalStorageDirectory() + "/MyCamera/";
    private static final String TAG = "CameraActivity";
    private int camera_way;
    private int carema_option;
    private boolean light_switch;
    private boolean sync_switch;
    private boolean infrared_light_switch;
    private boolean face_switch;

    private List<SurfaceView> surfaceViews = new ArrayList<>();
    private List<SurfaceHolder> surfaceHolders = new ArrayList<>();
    private List<Camera> cameras = new ArrayList<>();
    private Camera.Parameters parameters;
    private CameraDetectManager cameraDetectManager;
    private final int canLightChannel = 1;
    private int nowCameraId = 0;
    private int tempPhoto = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!parseData()) {
            showToast("参数异常！");
            finish();
        }
        if (0 == camera_way) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setContentView(R.layout.activity_camera2);
            SurfaceView surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
            surfaceViews.add(surfaceView);
            SurfaceHolder surfaceHolder = surfaceView.getHolder();
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            surfaceHolder.addCallback(new surfaceCallback(0, surfaceView));
            surfaceHolders.add(surfaceHolder);
            nowCameraId = 1;
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setContentView(R.layout.activity_camera);
            SurfaceView surfaceView1 = (SurfaceView) findViewById(R.id.surfaceview1);
            surfaceViews.add(surfaceView1);
            SurfaceHolder surfaceHolder1 = surfaceView1.getHolder();
            surfaceHolder1.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            surfaceHolder1.addCallback(new surfaceCallback(0, surfaceView1));
            surfaceHolders.add(surfaceHolder1);

            SurfaceView surfaceView2 = (SurfaceView) findViewById(R.id.surfaceview2);
            surfaceViews.add(surfaceView2);
            SurfaceHolder surfaceHolder2 = surfaceView2.getHolder();
            surfaceHolder2.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            surfaceHolder2.addCallback(new surfaceCallback(1, surfaceView2));
            surfaceHolders.add(surfaceHolder2);
        }
        View takePhoto = findViewById(R.id.btnTakePhoto);
        if (1 == carema_option) {
            takePhoto.setVisibility(View.GONE);
        }
        takePhoto.setOnClickListener(this);
        View changeCamera = findViewById(R.id.btnSwitch);
        if (1 == camera_way) {
            changeCamera.setVisibility(View.GONE);
        }
        changeCamera.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPreview();
        Light.closeInfrared();
        Light.close();
        if (surfaceHolders != null) {
            surfaceHolders = null;
        }
        if (surfaceViews != null) {
            for (SurfaceView surfaceView : surfaceViews) {
                surfaceView.destroyDrawingCache();
            }
            surfaceViews = null;
        }
        if (cameraDetectManager != null) {
            cameraDetectManager.release();
        }
    }

    private boolean parseData() {
        Intent intent = getIntent();
        if (intent == null) {
            return false;
        }
        camera_way = intent.getIntExtra(MainActivity.CAMERA_WAY, 1);
        carema_option = intent.getIntExtra(MainActivity.CAMERA_OPTION, 0);
        light_switch = intent.getBooleanExtra(MainActivity.CAMERA_LIGHT, true);
        sync_switch = intent.getBooleanExtra(MainActivity.CAMERA_SYNC, true);
        infrared_light_switch = intent.getBooleanExtra(MainActivity.CAMERA_INFRARED, true);
        face_switch = intent.getBooleanExtra(MainActivity.CAMERA_FACE, false);
        return true;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (R.id.btnTakePhoto == id) {
            if (sync_switch) {
                int cameraNum = cameras.size();
                for (int i = 0; i < cameraNum; i++) {
                    takePhoto(cameras.get(i), surfaceHolders.get(i));
                }
            } else {
                if (tempPhoto == cameras.size()) {
                    tempPhoto = 0;
                } else if (tempPhoto == cameras.size() - 1) {
                    showToast("左右相机均已拍摄！");
                }
                takePhoto(cameras.get(tempPhoto), surfaceHolders.get(tempPhoto));
                tempPhoto++;
            }

        } else if (R.id.btnSwitch == id) {
            switchCamera();
        }
    }

    class surfaceCallback implements SurfaceHolder.Callback {
        private int cameraId;
        private Camera camera;
        private SurfaceView surfaceView;

        public surfaceCallback(int cameraId, SurfaceView surfaceView) {
            this.cameraId = cameraId;
            this.surfaceView = surfaceView;
            if (canLightChannel == cameraId && face_switch) {
                initCameraLight();
            }
        }

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            camera = openCamera(cameraId, surfaceHolder);
            if (canLightChannel == cameraId) {
                camera.setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] bytes, Camera camera) {
                        if (canLightChannel == cameraId && face_switch) {
                            cameraDetectManager.detect(bytes, surfaceView.getWidth(), surfaceView.getHeight());
                        }
                    }
                });
            }
            cameras.add(camera);
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean b, Camera camera) {
                    if (b) {
                        initCamera(camera);
                        camera.cancelAutoFocus();
                    }
                }
            });
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        }

    }

    private Camera openCamera(int num, SurfaceHolder holder) {
        Log.v("popo9090", String.valueOf(num));
        int cameraNum = Camera.getNumberOfCameras();
        if (cameraNum <= 0 || cameraNum < num + 1) {
            Toast.makeText(this, getResources().getString(R.string.not_support),
                    Toast.LENGTH_SHORT).show();
            finish();
        }
        detScope();
        Camera camera = Camera.open(num);
        try {
            // 设置预览监听
            camera.setPreviewDisplay(holder);
            Camera.Parameters parameters = camera.getParameters();
            lightSwitch(num, parameters);
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

    /**
     * 打开预览
     */
    private void startPreview(Camera camera, SurfaceHolder surfaceHolder) {
        if (camera == null) {
            return;
        }
        try {
            camera.setDisplayOrientation(90);
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止预览释放相机
     */
    private void stopPreview() {
        if (cameras != null && cameras.size() > 0) {
            for (Camera camera : cameras) {
                camera.setPreviewCallback(null);
                camera.stopPreview();
                camera.release();
            }
            cameras = null;
        }
    }

    /**
     * 相机初始化
     *
     * @param camera
     */
    private void initCamera(Camera camera) {
        parameters = camera.getParameters();
        parameters.setPictureFormat(PixelFormat.JPEG);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);// 连续对焦
        setDisplay(parameters, camera);
        camera.setParameters(parameters);
        camera.startPreview();
        camera.cancelAutoFocus();// 如果要实现连续的自动对焦，这一句必须加上
    }

    /**
     * 相机探测初始化
     */
    private void initCameraLight() {
        cameraDetectManager = new CameraDetectManager(this);
        cameraDetectManager.setDelayTime(10 * 1000);
        cameraDetectManager.setVisibleDetectThreshold(50);
        cameraDetectManager.init(new CommonListener() {
            @Override
            public void success() {
                Light.openInfrared();
                Log.e(TAG, "success");
            }

            @Override
            public void fail() {
                Log.e(TAG, "fail");
                cameraDetectManager.release();
            }
        }, new FaceDetectListener() {
            @Override
            public void success() {
                //检测到人脸
                Log.e(TAG, "detectSuccess");
                Light.open();
            }

            @Override
            public void fail() {
                Log.e(TAG, "detectFail");
            }

        });
    }

    private void setDisplay(Camera.Parameters parameters, Camera camera) {
        if (Integer.parseInt(Build.VERSION.SDK) >= 8) {
            setDisplayOrientation(camera, 0);
        } else {
            parameters.setRotation(0);
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

    /**
     * 拍照
     */
    private void takePhoto(Camera camera, final SurfaceHolder surfaceHolder) {
        camera.takePicture(new Camera.ShutterCallback() {
            @Override
            public void onShutter() {
                //按下快门
            }
        }, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

            }
        }, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                savePicture(data);
                startPreview(camera, surfaceHolder);
            }
        });
    }

    /**
     * 保存图像
     *
     * @param data
     */
    private void savePicture(final byte[] data) {
        PermisstionUtil.requestPermissions(this, PermisstionUtil.STORAGE, 101, "正在获取读写权限", new PermisstionUtil.OnPermissionResult() {
            @Override
            public void granted(int requestCode) {
                showToast(getResources().getString(R.string.saving));
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Matrix matrix = new Matrix();
                            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                            BitmapUtil.save(bitmap, FILEPATH + System.currentTimeMillis() + ".jpg");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showToast(getResources().getString(R.string.save_success));
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }

            @Override
            public void denied(int requestCode) {
                showToast(getResources().getString(R.string.scope_write_bid));
            }
        });
    }

    /**
     * 切换摄像头
     */
    private void switchCamera() {
        int count = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < count; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (nowCameraId == Camera.CameraInfo.CAMERA_FACING_BACK && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                //后置变前置
                stopPreview();
                nowCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
                if (surfaceHolders.size() == 1) {
                    openCamera(nowCameraId, surfaceHolders.get(0));
                }
                break;
            } else if (nowCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT && cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                //前置变后置
                stopPreview();
                nowCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
                if (surfaceHolders.size() == 1) {
                    openCamera(nowCameraId, surfaceHolders.get(0));
                }
                break;
            }
        }
    }

    /**
     * 灯控制
     *
     * @param cameraId
     * @param parameters
     */
    private void lightSwitch(int cameraId, Camera.Parameters parameters) {
        if (cameraId == canLightChannel) {
            if (infrared_light_switch) {
                Light.openInfrared();
            }
            if (light_switch) {
                Light.open();
            }
        } else {
            Light.closeInfrared();
            Light.close();
        }
        if (parameters != null) {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        }
    }

    /**
     * 权限检测
     */
    private void detScope() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            System.out.println("ok");
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }
    }

    /**
     * 信息提示
     *
     * @param msg
     */
    private void showToast(String msg) {
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show();
    }
}
