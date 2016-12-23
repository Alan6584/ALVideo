package com.alan.alvideo.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;

import com.alan.alvideo.camera.CameraController;
import com.alan.alvideo.camera.CameraRecordRenderer;
import com.alan.alvideo.camera.CameraUtils;
import com.alan.alvideo.filter.FilterManager.FilterType;
import com.alan.alvideo.video.EncoderConfig;

import java.lang.ref.WeakReference;

public class CameraSurfaceView extends GLSurfaceView implements SurfaceTexture.OnFrameAvailableListener {

    private CameraHandler mBackgroundHandler;
    private HandlerThread mHandlerThread;
    private CameraRecordRenderer mCameraRenderer;

    public CameraSurfaceView(Context context) {
        super(context);
        init();
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setEGLContextClientVersion(2);//设置EGL版本，不设置渲染不出图像来
        mHandlerThread = new HandlerThread("CameraHandlerThread");
        mHandlerThread.start();

        mBackgroundHandler = new CameraHandler(mHandlerThread.getLooper(), this);
        mCameraRenderer = new CameraRecordRenderer(mBackgroundHandler);

        setRenderer(mCameraRenderer);//GLSurfaceView支持自定义Render，此处绑定自定义render
        setRenderMode(RENDERMODE_WHEN_DIRTY);//当camera有数据时才渲染，故可以将渲染模式设置为RENDERMODE_WHEN_DIRTY，有数据时通知其渲染，而不必设置为连续渲染
    }

    /**
     * 设置录制状态，开始录制、停止录制
     * @param recordingEnabled
     */
    public void setRecordingEnabled(boolean recordingEnabled) {
        if (mCameraRenderer != null) {
            mCameraRenderer.setRecordingEnabled(recordingEnabled);
        }
    }

    /**
     * 设置编码器配置，携带录制文件的宽高、输出文件等信息
     * @param encoderConfig
     */
    public void setEncoderConfig(EncoderConfig encoderConfig) {
        if (mCameraRenderer != null) {
            mCameraRenderer.setEncoderConfig(encoderConfig);
        }
    }

    /**
     * 更换滤镜
     * @param filterType
     */
    public void changeFilter(FilterType filterType) {
        if (mCameraRenderer != null) {
            mCameraRenderer.changeFilter(filterType);
        }
    }

    @Override
    public void onPause() {
        mBackgroundHandler.removeCallbacksAndMessages(null);
        CameraController.getInstance().release();
        queueEvent(new Runnable() {
            @Override
            public void run() {
                mCameraRenderer.stopRender();
            }
        });
        super.onPause();
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    public void onDestroy() {
        mBackgroundHandler.removeCallbacksAndMessages(null);
        if (!mHandlerThread.isInterrupted()) {
            try {
                mHandlerThread.quit();
                mHandlerThread.interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
    }


    /**
     * 处理camera相关的消息队列
     */
    public static class CameraHandler extends Handler {
        public static final int SETUP_CAMERA = 1001;
        public static final int CONFIGURE_CAMERA = 1002;
        public static final int START_CAMERA_PREVIEW = 1003;
        public static final int STOP_CAMERA_PREVIEW = 1004;

        private WeakReference<CameraSurfaceView> surfaceViewWeakReference;

        public CameraHandler(Looper looper, CameraSurfaceView cameraSurfaceView) {
            super(looper);
            surfaceViewWeakReference = new WeakReference<>(cameraSurfaceView);
        }

        @Override
        public void handleMessage(final Message msg) {
            final CameraSurfaceView weakCameraSurfaceView = surfaceViewWeakReference.get();
            if (weakCameraSurfaceView == null) {
                return;
            }

            switch (msg.what) {
                case CameraHandler.SETUP_CAMERA: {
                    final int width = msg.arg1;
                    final int height = msg.arg2;
                    final SurfaceTexture surfaceTexture = (SurfaceTexture) msg.obj;
                    surfaceTexture.setOnFrameAvailableListener(weakCameraSurfaceView);

                    weakCameraSurfaceView.mBackgroundHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            CameraController.getInstance()
                                    .setupCamera(surfaceTexture);
                            weakCameraSurfaceView.mBackgroundHandler.sendMessage(weakCameraSurfaceView.mBackgroundHandler.obtainMessage(
                                    CameraSurfaceView.CameraHandler.CONFIGURE_CAMERA, width, height));
                        }
                    });
                }
                break;
                case CameraHandler.CONFIGURE_CAMERA: {
                    final int width = msg.arg1;
                    final int height = msg.arg2;
                    Camera.Size previewSize = CameraUtils.getOptimalPreviewSize(
                            CameraController.getInstance().getCameraParameters(),
                            width, height);

                    Log.e("Alan", "width = " + width + "--->>>height = " + height);
                    Log.e("Alan", "previewSize.width = " + previewSize.width + "--->>>previewSize.height = " + previewSize.height);

                    CameraController.getInstance().configureCameraParameters(previewSize);
                    weakCameraSurfaceView.mBackgroundHandler.sendEmptyMessage(CameraHandler.START_CAMERA_PREVIEW);
                }
                break;

                case CameraHandler.START_CAMERA_PREVIEW:
                    weakCameraSurfaceView.mBackgroundHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            CameraController.getInstance().startCameraPreview();
                        }
                    });

                    break;
                case CameraHandler.STOP_CAMERA_PREVIEW:
                    weakCameraSurfaceView.mBackgroundHandler.post(new Runnable() {
                        @Override public void run() {
                            CameraController.getInstance().stopCameraPreview();
                        }
                    });
                    break;

                default:
                    break;
            }
        }
    }


}