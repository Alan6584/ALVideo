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
import com.alan.alvideo.camera.CameraRecordRenderer;
import com.alan.alvideo.filter.FilterManager.FilterType;
import com.alan.alvideo.util.FileUtil;
import com.alan.alvideo.video.EncoderConfig;
import com.alan.alvideo.video.TextureMovieEncoder;
import com.alan.alvideo.view.CameraSurfaceView;

import java.io.File;

public class VideoActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    private CameraSurfaceView mCameraSurfaceView;
    private TextView curStatusTV;
    private Button mRecordButton;
    private boolean mIsRecordEnabled;
    private String curFileName;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        mCameraSurfaceView = (CameraSurfaceView) findViewById(R.id.camera);
        curStatusTV = (TextView) findViewById(R.id.current_status);

        Spinner spinner = (Spinner) findViewById(R.id.camera_filter_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.cameraFilterNames, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner.
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        mRecordButton = (Button) findViewById(R.id.record);
        mRecordButton.setOnClickListener(this);

        mIsRecordEnabled = TextureMovieEncoder.getInstance().isRecording();
        updateRecordButton();
    }

    @Override protected void onResume() {
        super.onResume();
        mCameraSurfaceView.onResume();
        updateRecordButton();
    }

    @Override protected void onPause() {
        mCameraSurfaceView.onPause();
        super.onPause();
    }

    @Override protected void onDestroy() {
        mCameraSurfaceView.onDestroy();
        super.onDestroy();
    }

    @Override public void onClick(View v) {
        switch (v.getId()) {
            case R.id.record:
                if (!mIsRecordEnabled) {
                    curFileName = "video-" + System.currentTimeMillis() + ".mp4";
                    mCameraSurfaceView.queueEvent(new Runnable() {
                        @Override public void run() {
                            CameraRecordRenderer renderer = mCameraSurfaceView.getRenderer();
                            renderer.setEncoderConfig(new EncoderConfig(new File(
                                    FileUtil.getCacheDirectory(VideoActivity.this, true),
                                    curFileName), 480, 640,
                                    1024 * 1024 /* 1 Mb/s */));
                        }
                    });
                }
                mIsRecordEnabled = !mIsRecordEnabled;
                if (mIsRecordEnabled){
                    curStatusTV.setText(getString(R.string.recording_status));
                }else {
                    curStatusTV.setText(getString(R.string.recording_saved)
                            + new File(FileUtil.getCacheDirectory(VideoActivity.this, true), curFileName).getAbsolutePath());
                }
                mCameraSurfaceView.queueEvent(new Runnable() {
                    @Override public void run() {
                        mCameraSurfaceView.getRenderer().setRecordingEnabled(mIsRecordEnabled);
                    }
                });
                updateRecordButton();
                break;
        }
    }

    public void updateRecordButton() {
        mRecordButton.setText(
                getString(mIsRecordEnabled ? R.string.record_stop : R.string.record_start));
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Spinner spinner = (Spinner) parent;
        final int filterNum = spinner.getSelectedItemPosition();
        Log.d("Alan", "onItemSelected: " + filterNum);

        switch (filterNum) {
            case 0:
                mCameraSurfaceView.changeFilter(FilterType.Normal);
                break;
            case 1:
                mCameraSurfaceView.changeFilter(FilterType.GRAYSCALE);
                break;
            case 2:
                mCameraSurfaceView.changeFilter(FilterType.STARMAKER);
                break;
            case 3:
                mCameraSurfaceView.changeFilter(FilterType.SEPIA);
                break;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}
