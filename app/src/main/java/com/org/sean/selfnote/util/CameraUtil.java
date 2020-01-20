package com.org.sean.selfnote.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.org.sean.selfnote.R;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * <pre>
 * author : Sean
 * e-mail : seanpower@foxmail.com
 * time   : 2020/01/16
 * </pre>
 */
public class CameraUtil {
    private Activity activity;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private CameraManager cameraManager;
    private HandlerThread handlerThread;
    private Handler handler;
    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private CaptureRequest.Builder previewBuilder;
    private CaptureRequest.Builder captureBuilder;
    private int cameraId;
    private ImageReader imageReader;
    private Size previewSize;
    private boolean openLight;


    public CameraUtil(@NonNull Activity activity, @NonNull SurfaceView surfaceView, @NonNull int cameraId, @NonNull boolean openLight) {
        this.activity = activity;
        this.surfaceView = surfaceView;
        this.cameraId = cameraId;
        this.openLight = openLight;
    }

    public void run() {
        surfaceHolder = surfaceView.getHolder();
        initData();
    }

    /**
     * 打开相机回调
     */
    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            //相机开启，打开预览
            cameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            //相机关闭
            camera.close();
            cameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            //相机报错
            camera.close();
            cameraDevice = null;
        }
    };

    /**
     * 创建session回调
     */
    private CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            try {
                cameraCaptureSession = session;
                //自动对焦
                previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                cameraCaptureSession.setRepeatingRequest(previewBuilder.build(), null, handler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            session.close();
            cameraCaptureSession = null;
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    //拍完照回调
    private CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            super.onCaptureProgressed(session, request, partialResult);
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            try {
                //自动对焦
                captureBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_CANCEL);
                //重新打开预览
                session.setRepeatingRequest(previewBuilder.build(), null, handler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            cameraCaptureSession.close();
            cameraCaptureSession = null;
            cameraDevice.close();
            cameraDevice = null;
        }
    };

    /**
     * 初始化
     */
    private void initData() {
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                openCamera();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                closeCamera();
            }
        });
        cameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        //Camera2全程异步
        handlerThread = new HandlerThread(getThreadName());
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    /**
     * 打开相机
     */
    private void openCamera() {
        PermisstionUtil.requestPermissions(activity, PermisstionUtil.CAMERA, 101, "正在请求拍照权限", new PermisstionUtil.OnPermissionResult() {
            @Override
            public void granted(int requestCode) {
                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                try {
                    //获取属性CameraDevice属性描述
                    CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(String.valueOf(cameraId));
                    //获取摄像头支持的配置属性
                    StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    previewSize = getMaxSize(map.getOutputSizes(SurfaceHolder.class));
                    initImageReader();
                    //第一个参数指定哪个摄像头，第二个参数打开摄像头的状态回调，第三个参数是运行在哪个线程(null是当前线程)
                    cameraManager.openCamera(String.valueOf(cameraId), stateCallback, handler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void denied(int requestCode) {

            }
        });
    }

    /**
     * 关闭相机
     */
    private void closeCamera() {
        //关闭捕捉会话
        if (cameraCaptureSession != null) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
        //关闭相机
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        //关闭拍照处理器
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
    }

    /**
     * 切换摄像头
     */
    public void switchCamera() {
        try {
            for (String LCameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(LCameraId);
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                Size maxSize = getMaxSize(map.getOutputSizes(SurfaceHolder.class));
                if (cameraId == CameraCharacteristics.LENS_FACING_BACK && characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT) {
                    //前置转后置
                    previewSize = maxSize;
                    cameraId = CameraCharacteristics.LENS_FACING_FRONT;
                    cameraDevice.close();
                    openCamera();
                    break;
                } else if (cameraId == CameraCharacteristics.LENS_FACING_FRONT && characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                    //后置转前置
                    previewSize = maxSize;
                    cameraId = CameraCharacteristics.LENS_FACING_BACK;
                    cameraDevice.close();
                    openCamera();
                    break;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 初始化拍照处理器
     */
    private void initImageReader() {
        imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), ImageFormat.JPEG, 1);
        //监听ImageReader时间，有图像数据可用时回调，参数就是帧数据
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage();
                //将帧数据转换成字节数组
                ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
                byte[] data = new byte[byteBuffer.remaining()];
                byteBuffer.get(data);
                //保存照片
                savePicture(data);
                image.close();
            }
        }, handler);
    }

    /**
     * 获取最大预览尺寸
     *
     * @param outputSizes
     * @return
     */
    private Size getMaxSize(Size[] outputSizes) {
        Size sizeMax = null;
        if (outputSizes != null) {
            sizeMax = outputSizes[0];
            for (Size size : outputSizes) {
                if (size.getWidth() * size.getHeight() > sizeMax.getWidth() * sizeMax.getHeight()) {
                    sizeMax = size;
                }
            }
        }
        return sizeMax;
    }

    /**
     * 开始预览
     */
    private void startPreview() {
        try {
            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            lightSwitch(previewBuilder, openLight);
            //设置预览数据输出界面
            previewBuilder.addTarget(surfaceHolder.getSurface());
            //创建相机捕获会话，
            //第一个参数是捕获数据的输出Surface列表，第二个参数是CameraCaptureSession状态回调接口，第三个参数是在哪个线程(null是当前线程)
            cameraDevice.createCaptureSession(Arrays.asList(surfaceHolder.getSurface(), imageReader.getSurface()), sessionStateCallback, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 拍照
     */
    public void takePhoto() {
        try {
            captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());
            //自动对焦
            lightSwitch(captureBuilder, openLight);
//            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            cameraCaptureSession.stopRepeating();
            //拍照
            cameraCaptureSession.capture(captureBuilder.build(), captureCallback, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取线程名
     *
     * @return
     */
    private String getThreadName() {
        return "camera".concat(String.valueOf(cameraId));
    }

    /**
     * 保存图像
     *
     * @param data
     */
    private void savePicture(final byte[] data) {
        PermisstionUtil.requestPermissions(activity, PermisstionUtil.STORAGE, 101,
                activity.getResources().getString(R.string.ob_scope_write), new PermisstionUtil.OnPermissionResult() {
                    @Override
                    public void granted(int requestCode) {
                        Toast.makeText(activity, activity.getResources().getString(R.string.saving), Toast.LENGTH_LONG).show();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Matrix matrix = new Matrix();
                                    matrix.setRotate(cameraId == CameraCharacteristics.LENS_FACING_FRONT ? 90 : 270);
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                                    if (BitmapUtil.save(bitmap, PathUtil.getStoragePath(cameraId) + System.currentTimeMillis() + ".jpg")) {
                                        showToast(activity.getResources().getString(R.string.save_success));
                                    } else {
                                        showToast(activity.getResources().getString(R.string.save_failed));
                                    }

                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).start();
                    }

                    @Override
                    public void denied(int requestCode) {
                        Toast.makeText(activity, activity.getResources().getString(R.string.scope_write_bid), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void lightSwitch(CaptureRequest.Builder builder, boolean open) {
        if (builder != null) {
            builder.set(CaptureRequest.FLASH_MODE, open ? CaptureRequest.FLASH_MODE_TORCH : CaptureRequest.FLASH_MODE_OFF);
        }
    }

    private void showToast(final String msg) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
