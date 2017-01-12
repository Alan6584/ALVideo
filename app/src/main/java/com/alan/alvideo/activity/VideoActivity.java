package com.alan.alvideo.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.alan.alvideo.R;
import com.alan.alvideo.filter.FilterManager.FilterType;
import com.alan.alvideo.util.FileUtil;
import com.alan.alvideo.video.EncoderConfig;
import com.alan.alvideo.video.TextureMovieEncoder;
import com.alan.alvideo.view.CameraSurfaceView;

import java.io.File;

/**
 * Created by wangjianjun on 17/01/09.
 * alanwang6584@gmail.com
 */
public class VideoActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    private CameraSurfaceView cameraSurfaceView;
    private TextView curStatusTV;
    private Button recordBtn;
    private boolean isRecordEnabled;
    private File curRecordFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        initView();
        isRecordEnabled = TextureMovieEncoder.getInstance().isRecording();
    }

    //初始化界面
    private void initView() {
        cameraSurfaceView = (CameraSurfaceView) findViewById(R.id.camera);
        curStatusTV = (TextView) findViewById(R.id.current_status);

        //初始化滤镜选择器
        Spinner spinner = (Spinner) findViewById(R.id.camera_filter_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.cameraFilterNames, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        recordBtn = (Button) findViewById(R.id.record);
        recordBtn.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraSurfaceView.onResume();//与Activity的生命周期保持一致
    }

    @Override
    protected void onPause() {
        cameraSurfaceView.onPause();//与Activity的生命周期保持一致
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        cameraSurfaceView.onDestroy();//与Activity的生命周期保持一致
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.record:
                if (!isRecordEnabled) {
                    //开启录制流程
                    String curFileName = "video-" + System.currentTimeMillis() + ".mp4";
                    curRecordFile = new File(FileUtil.getCacheDirectory(VideoActivity.this, true), curFileName);
                    cameraSurfaceView.queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            cameraSurfaceView.setEncoderConfig(new EncoderConfig(curRecordFile, 480, 640,
                                    1024 * 1024 /* 1 Mb/s */));
                        }
                    });
                }
                isRecordEnabled = !isRecordEnabled;
                cameraSurfaceView.queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        cameraSurfaceView.setRecordingEnabled(isRecordEnabled);
                    }
                });
                updateUIRecordStatus();
                break;
        }
    }

    /**
     * 根据当前录制状态更新界面状态
     */
    public void updateUIRecordStatus() {
        curStatusTV.setText(isRecordEnabled ? getString(R.string.recording_status) : getString(R.string.recording_saved) + curRecordFile.getAbsolutePath());
        recordBtn.setText(getString(isRecordEnabled ? R.string.record_stop : R.string.record_start));
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Spinner spinner = (Spinner) parent;
        final int filterNum = spinner.getSelectedItemPosition();
        Log.d("Alan", "onItemSelected: " + filterNum);

        //根据选择的滤镜实时更新滤镜效果
        switch (filterNum) {
            case 0:
                cameraSurfaceView.changeFilter(FilterType.NORMAL);
                break;
            case 1:
                cameraSurfaceView.changeFilter(FilterType.GRAYSCALE);
                break;
            case 2:
                cameraSurfaceView.changeFilter(FilterType.STARMAKER);
                break;
            case 3:
                cameraSurfaceView.changeFilter(FilterType.SEPIA);
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
