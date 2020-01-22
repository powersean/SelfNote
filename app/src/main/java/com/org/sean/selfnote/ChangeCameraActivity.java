package com.org.sean.selfnote;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.org.sean.selfnote.util.CameraUtil;
import com.org.sean.selfnote.util.PermisstionUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * <pre>
 * author : Sean
 * e-mail : seanpower@foxmail.com
 * time   : 2020/01/19
 * </pre>
 */
public class ChangeCameraActivity extends AppCompatActivity implements View.OnClickListener {
    private int cameraWay;
    private boolean isOpenLight;
    private boolean isSync;
    private boolean isInfrafed;
    private List<CameraUtil> cameraUtils = new ArrayList<>();
    private int count;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initCamera();
    }

    private void initView() {
        Intent intent = getIntent();
        if (intent == null) {
            finish();
        }
        cameraWay = intent.getIntExtra(MainActivity.CAMERA_WAY, 0);
        if (0 == cameraWay) {
            setContentView(R.layout.activity_camera2);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setContentView(R.layout.activity_camera);
        }
        int option = intent.getIntExtra(MainActivity.CAMERA_OPTION, 0);
        View takePhoto = findViewById(R.id.btnTakePhoto);
        View changeCamera = findViewById(R.id.btnSwitch);
        if (1 == option) {
            takePhoto.setVisibility(View.GONE);
            if (1 == cameraWay) {
                changeCamera.setVisibility(View.GONE);
            }
        } else {
            takePhoto.setVisibility(View.VISIBLE);
            takePhoto.setOnClickListener(this);
        }
        changeCamera.setOnClickListener(this);
        isOpenLight = intent.getBooleanExtra(MainActivity.CAMERA_LIGHT, true);
        isSync = intent.getBooleanExtra(MainActivity.CAMERA_SYNC, true);
        isInfrafed = intent.getBooleanExtra(MainActivity.CAMERA_INFRARED, true);
    }

    private void initCamera() {
        if (0 == cameraWay) {
            SurfaceView surfaceView = findViewById(R.id.surfaceView);
            CameraUtil cameraUtil = new CameraUtil(this, surfaceView, 0, isOpenLight);
            cameraUtils.add(cameraUtil);
            cameraUtil.setInfraredLight(isInfrafed).run();
        } else {
            SurfaceView surfaceView1 = findViewById(R.id.surfaceview1);
            CameraUtil cameraUtil1 = new CameraUtil(this, surfaceView1, 0, isOpenLight);
            cameraUtils.add(cameraUtil1);
            cameraUtil1.setInfraredLight(isInfrafed).run();
            SurfaceView surfaceView2 = findViewById(R.id.surfaceview2);
            CameraUtil cameraUtil2 = new CameraUtil(this, surfaceView2, 1, isOpenLight);
            cameraUtils.add(cameraUtil2);
            cameraUtil2.setInfraredLight(isInfrafed).run();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermisstionUtil.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (R.id.btnTakePhoto == id) {
            if (!isSync) {
                CameraUtil cameraUtil = cameraUtils.get(count);
                if (cameraUtil != null) {
                    cameraUtil.takePhoto();
                }
                count++;
                if (count == cameraUtils.size()) {
                    count = 0;
                }
            } else {
                for (CameraUtil cameraUtil : cameraUtils) {
                    if (cameraUtil != null) {
                        cameraUtil.takePhoto();
                    }
                }
            }
        } else if (R.id.btnSwitch == id) {
            if (0 == cameraWay) {
                cameraUtils.get(0).switchCamera();
            }
        }
    }
}
