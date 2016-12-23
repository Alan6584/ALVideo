package com.alan.alvideo.camera;

import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLSurfaceView;

import com.alan.alvideo.filter.FilterManager;
import com.alan.alvideo.filter.FilterManager.FilterType;
import com.alan.alvideo.gles.FullFrameRect;
import com.alan.alvideo.gles.GlUtil;
import com.alan.alvideo.video.EncoderConfig;
import com.alan.alvideo.video.TextureMovieEncoder;
import com.alan.alvideo.view.CameraSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraRecordRenderer implements GLSurfaceView.Renderer {

    private static final int RECORDING_OFF = 0;
    private static final int RECORDING_ON = 1;
    private static final int RECORDING_RESUMED = 2;

    private int mTextureId = GlUtil.NO_TEXTURE;
    private FullFrameRect mFullScreen;
    private SurfaceTexture mSurfaceTexture;
    private final float[] mSTMatrix = new float[16];

    private FilterType mCurrentFilterType;
    private FilterType mNewFilterType;
    private EncoderConfig mEncoderConfig;
    private TextureMovieEncoder mVideoEncoder;
    private final CameraSurfaceView.CameraHandler mCameraHandler;

    private boolean mRecordingEnabled;
    private int mRecordingStatus;

    public CameraRecordRenderer(CameraSurfaceView.CameraHandler cameraHandler) {
        mCameraHandler = cameraHandler;
        mCurrentFilterType = mNewFilterType = FilterType.NORMAL;
        mVideoEncoder = TextureMovieEncoder.getInstance();
    }

    /**
     * 设置编码器配置，携带录制文件的宽高、输出文件等信息
     * @param encoderConfig
     */
    public void setEncoderConfig(EncoderConfig encoderConfig) {
        mEncoderConfig = encoderConfig;
    }

    /**
     * 设置录制状态，开始录制、停止录制
     * @param recordingEnabled
     */
    public void setRecordingEnabled(boolean recordingEnabled) {
        mRecordingEnabled = recordingEnabled;
    }

    /**
     * 停止渲染
     */
    public void stopRender() {
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
            mSurfaceTexture = null;
        }
        if (mFullScreen != null) {
            mFullScreen.release(false);
            mFullScreen = null;
        }
    }

    /**
     * 更换滤镜
     * @param filterType
     */
    public void changeFilter(FilterType filterType) {
        mNewFilterType = filterType;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mRecordingEnabled = mVideoEncoder.isRecording();
        if (mRecordingEnabled) {
            mRecordingStatus = RECORDING_RESUMED;
        } else {
            mRecordingStatus = RECORDING_OFF;
            mVideoEncoder.initFilter(mCurrentFilterType);
        }
        //初始化渲染器，并获取TextureId创建SurfaceTexture，后面会将该SurfaceTexture与camera绑定
        mFullScreen = new FullFrameRect(FilterManager.getCameraFilter(mCurrentFilterType));
        mTextureId = mFullScreen.createTextureObject();
        mSurfaceTexture = new SurfaceTexture(mTextureId);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (gl != null) {
            gl.glViewport(0, 0, width, height);
        }
        mCameraHandler.sendMessage(mCameraHandler.obtainMessage(
                CameraSurfaceView.CameraHandler.SETUP_CAMERA, width, height, mSurfaceTexture));
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        mSurfaceTexture.updateTexImage();
        if (mNewFilterType != mCurrentFilterType) {//如果滤镜改变，则更新滤镜
            mFullScreen.changeProgram(FilterManager.getCameraFilter(mNewFilterType));
            mCurrentFilterType = mNewFilterType;
        }
        mSurfaceTexture.getTransformMatrix(mSTMatrix);
        mFullScreen.drawFrame(mTextureId, mSTMatrix);

        encoderDrawFrame(mTextureId, mSTMatrix, mSurfaceTexture.getTimestamp());
    }

    /**
     * 通知编码器绘制video frame
     * @param textureId 纹理ID，与相机预览的纹理绑定，获取纹理数据
     * @param texMatrix 纹理矩阵
     * @param timestamp 时间戳
     */
    private void encoderDrawFrame(int textureId, float[] texMatrix, long timestamp) {
        if (mRecordingEnabled && mEncoderConfig != null) {
            switch (mRecordingStatus) {
                case RECORDING_OFF:
                    mEncoderConfig.updateEglContext(EGL14.eglGetCurrentContext());
                    mVideoEncoder.startRecording(mEncoderConfig);
                    mVideoEncoder.setTextureId(textureId);
                    mRecordingStatus = RECORDING_ON;

                    break;
                case RECORDING_RESUMED:
                    mVideoEncoder.updateSharedContext(EGL14.eglGetCurrentContext());
                    mVideoEncoder.setTextureId(textureId);
                    mRecordingStatus = RECORDING_ON;
                    break;
                case RECORDING_ON:
                    // yay
                    break;
                default:
                    throw new RuntimeException("unknown status " + mRecordingStatus);
            }
        } else {
            switch (mRecordingStatus) {
                case RECORDING_ON:
                case RECORDING_RESUMED:
                    mVideoEncoder.stopRecording();
                    mRecordingStatus = RECORDING_OFF;
                    break;
                case RECORDING_OFF:
                    // yay
                    break;
                default:
                    throw new RuntimeException("unknown status " + mRecordingStatus);
            }
        }

        mVideoEncoder.updateFilter(mCurrentFilterType);
        mVideoEncoder.frameAvailable(texMatrix, timestamp);
    }
}
