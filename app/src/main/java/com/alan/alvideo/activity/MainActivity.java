package com.alan.alvideo.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.alan.alvideo.R;
import com.alan.alvideo.util.FileUtil;
import com.alan.alvideo.util.RuntimePermissionsHelper;

import java.io.File;

/**
 * Created by wangjianjun on 17/01/09.
 * alanwang6584@gmail.com
 */
public class MainActivity extends Activity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText("just for testing \r");
        TextView cross = (TextView) findViewById(R.id.videoRecord);
        cross.setOnClickListener(this);
        TextView videoTv = (TextView) findViewById(R.id.videoPlay);
        videoTv.setOnClickListener(this);

        RuntimePermissionsHelper runtimePermissionsHelper = RuntimePermissionsHelper.create(this, null,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (!runtimePermissionsHelper.allPermissionsGranted()) {
            runtimePermissionsHelper.makeRequest();
        }

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.videoRecord:
                startActivity(new Intent(MainActivity.this, VideoActivity.class));
                break;
            case R.id.videoPlay:
                startActivity(new Intent(MainActivity.this, GLVideoPlayActivity.class));
                break;
            default:
                break;
        }
    }

}
