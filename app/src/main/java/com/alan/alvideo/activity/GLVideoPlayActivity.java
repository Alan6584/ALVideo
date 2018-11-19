package com.alan.alvideo.activity;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Environment;
import android.view.Surface;

import com.alan.alvideo.R;
import com.alan.alvideo.util.FileUtil;
import com.alan.alvideo.video.GLVideoRender;

import java.io.File;
import java.io.IOException;

/**
 * Author: wangjianjun.
 * Date: 17/1/9 12:03.
 * Mail: alanwang6584@gmail.com
 */

public class GLVideoPlayActivity extends Activity implements GLVideoRender.SurfaceListener{

    public static final String videoPath = Environment.getExternalStorageDirectory().getPath()+"/one.mp4";
    private GLSurfaceView vieoGlSurfaceView;
    private GLVideoRender glVideoRender;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_glvideo_play);

        //创建文件保存目录
        File rootDir = new File(FileUtil.getExternalDirectory(GLVideoPlayActivity.this), FileUtil.AL_DIR);
        if (!rootDir.exists()){
            rootDir.mkdirs();
        }

        vieoGlSurfaceView = (GLSurfaceView)findViewById(R.id.vieoGlSurfaceView);
        glVideoRender = new GLVideoRender();
        glVideoRender.setmSurfaceListener(this);
        vieoGlSurfaceView.setEGLContextClientVersion(2);
        vieoGlSurfaceView.setRenderer(glVideoRender);
    }

    @Override
    public void onPrepared() {
        playVideo(glVideoRender.getVideoTexture());
    }

    private void playVideo(SurfaceTexture videoTexture) {
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                }
            });
            Surface surface = new Surface(videoTexture);
            mediaPlayer.setSurface(surface);
            surface.release();
            try {
                mediaPlayer.setDataSource(videoPath);
                mediaPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
