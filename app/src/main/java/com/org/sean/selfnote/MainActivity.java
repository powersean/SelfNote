package com.org.sean.selfnote;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
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
    public static final String CAMERA_WAY = "camera_way";
    public static final String CAMERA_OPTION = "camera_option";
    public static final String CAMERA_LIGHT = "camera_light";
    public static final String CAMERA_SYNC = "camera_sync";

    private int camera_way = 0;
    private int camera_option = 0;
    private boolean light_switch = true;
    private boolean sync_switch = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Spinner spinner = findViewById(R.id.spinner);
        Spinner option = findViewById(R.id.option);
        final View light_view = findViewById(R.id.light);
        final View sync_view = findViewById(R.id.sync);
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
                    if (1 == camera_way) {
                        sync_view.setVisibility(View.VISIBLE);
                    }
                } else {
                    light_view.setVisibility(View.GONE);
                    sync_view.setVisibility(View.GONE);
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

        ((Switch) findViewById(R.id.switch_keep)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                sync_switch = b;
                if (!b) {
                    Toast.makeText(MainActivity.this, getResources().getString(R.string.sync_attention), Toast.LENGTH_SHORT).show();
                }
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
        Toast.makeText(this, getResources().getString(R.string.opening), Toast.LENGTH_SHORT).show();
        startActivity(intent);
    }
}
