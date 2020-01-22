package com.org.sean.selfnote;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.org.sean.selfnote.util.PathUtil;

public class MainActivity extends AppCompatActivity {
    public static final String CAMERA_WAY = "camera_way";//摄像头方式
    public static final String CAMERA_OPTION = "camera_option";//操作
    public static final String CAMERA_LIGHT = "camera_light";//补光灯控制
    public static final String CAMERA_SYNC = "camera_sync";//同步控制
    public static final String CAMERA_INFRARED = "camera_infrared";//红外开启
    public static final String CAMERA_FACE = "camera_face";//人脸识别

    private int camera_way = 0;
    private int camera_option = 0;
    private boolean light_switch = true;
    private boolean sync_switch = true;
    private boolean infrared_switch = true;
    private boolean face_switch = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Spinner spinner = findViewById(R.id.spinner);
        Spinner option = findViewById(R.id.option);
        final View light_view = findViewById(R.id.light);
        final View sync_view = findViewById(R.id.sync);
        final View infrared_view = findViewById(R.id.infrared_light);
        final View face_view = findViewById(R.id.face);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                camera_way = i;
                if (0 == i) {
                    sync_view.setVisibility(View.GONE);
                } else {
                    if (0 == camera_option) {
                        light_view.setVisibility(View.VISIBLE);
                        sync_view.setVisibility(View.VISIBLE);
                        infrared_view.setVisibility(View.VISIBLE);
                        face_view.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        option.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                camera_option = i;
                if (0 == i) {
                    light_view.setVisibility(View.VISIBLE);
                    infrared_view.setVisibility(View.VISIBLE);
                    face_view.setVisibility(View.VISIBLE);
                    if (1 == camera_way) {
                        sync_view.setVisibility(View.VISIBLE);
                    }
                } else {
                    light_view.setVisibility(View.GONE);
                    sync_view.setVisibility(View.GONE);
                    infrared_view.setVisibility(View.GONE);
                    face_view.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        ((Switch) findViewById(R.id.switch_light)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                light_switch = b;
            }
        });

        ((Switch) findViewById(R.id.switch_infrared_light)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                infrared_switch = b;
            }
        });

        ((Switch) findViewById(R.id.switch_keep)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                sync_switch = b;
                if (!b) {
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.sync_attention), Toast.LENGTH_SHORT).show();
                }
            }
        });

        ((Switch) findViewById(R.id.switch_face)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                face_switch = b;
            }
        });

        findViewById(R.id.open).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity();
            }
        });
        TextView textView = findViewById(R.id.attention_txt);
        String path = getResources().getString(R.string.font_camera) + "(1)：" + PathUtil.getStoragePath(1) + "\n"
                + getResources().getString(R.string.back_camera) + "(0)：" + PathUtil.getStoragePath(0);
        textView.setText(path);
    }

    private void startActivity() {
        Intent intent = new Intent(this, ChangeCameraActivity.class);
        intent.putExtra(CAMERA_WAY, camera_way);
        intent.putExtra(CAMERA_OPTION, camera_option);
        intent.putExtra(CAMERA_LIGHT, light_switch);
        intent.putExtra(CAMERA_SYNC, sync_switch);
        intent.putExtra(CAMERA_INFRARED, infrared_switch);
        intent.putExtra(CAMERA_FACE, face_switch);
        Toast.makeText(this, getResources().getString(R.string.opening), Toast.LENGTH_SHORT).show();
        startActivity(intent);
    }
}
