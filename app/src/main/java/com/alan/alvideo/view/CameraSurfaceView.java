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

import java.lang.ref.WeakReference;

public class CameraSurfaceView extends GLSurfaceView
        implements SurfaceTexture.OnFrameAvailableListener {

    private CameraHandler mBackgroundHandler;
    private HandlerThread mHandlerThread;
    private CameraRecordRenderer mCameraRenderer;

    public CameraSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {

        setEGLContextClientVersion(2);

        mHandlerThread = new HandlerThread("CameraHandlerThread");
        mHandlerThread.start();

        mBackgroundHandler = new CameraHandler(mHandlerThread.getLooper(), this);
        mCameraRenderer = new CameraRecordRenderer(context.getApplicationContext(), mBackgroundHandler);

        setRenderer(mCameraRenderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    public CameraRecordRenderer getRenderer() {
        return mCameraRenderer;
    }

    //public void setEncoderConfig(EncoderConfig encoderConfig) {
    //    if (mCameraRenderer != null) {
    //        mCameraRenderer.setEncoderConfig(encoderConfig);
    //    }
    //}

    @Override public void onResume() {
        super.onResume();
    }

    @Override public void onPause() {
        mBackgroundHandler.removeCallbacksAndMessages(null);
        CameraController.getInstance().release();
        queueEvent(new Runnable() {
            @Override public void run() {
                mCameraRenderer.notifyPausing();
            }
        });

        super.onPause();
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

    public void changeFilter(FilterType filterType) {
        mCameraRenderer.changeFilter(filterType);
    }

    @Override public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        requestRender();
    }

    public static class CameraHandler extends Handler {
        public static final int SETUP_CAMERA = 1001;
        public static final int CONFIGURE_CAMERA = 1002;
        public static final int START_CAMERA_PREVIEW = 1003;
        //public static final int STOP_CAMERA_PREVIEW = 1004;

        private WeakReference<CameraSurfaceView> surfaceViewWeakReference ;

        public CameraHandler(Looper looper, CameraSurfaceView cameraSurfaceView) {
            super(looper);
            surfaceViewWeakReference = new WeakReference<>(cameraSurfaceView);
        }

        @Override public void handleMessage(final Message msg) {
            final CameraSurfaceView weakCameraSurfaceView = surfaceViewWeakReference.get();
            if (weakCameraSurfaceView == null){
                return;
            }

            switch (msg.what) {
                case CameraHandler.SETUP_CAMERA: {
                    final int width = msg.arg1;
                    final int height = msg.arg2;
                    final SurfaceTexture surfaceTexture = (SurfaceTexture) msg.obj;
                    surfaceTexture.setOnFrameAvailableListener(weakCameraSurfaceView);

                    weakCameraSurfaceView.mBackgroundHandler.post(new Runnable() {
                        @Override public void run() {
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
                    if (previewSize != null) {
                        weakCameraSurfaceView.mCameraRenderer.setCameraPreviewSize(previewSize.height, previewSize.width);
                    }
                    weakCameraSurfaceView.mBackgroundHandler.sendEmptyMessage(CameraHandler.START_CAMERA_PREVIEW);
                }
                break;

                case CameraHandler.START_CAMERA_PREVIEW:
                    weakCameraSurfaceView.mBackgroundHandler.post(new Runnable() {
                        @Override public void run() {
                            CameraController.getInstance().startCameraPreview();
                        }
                    });

                    break;
                //case CameraHandler.STOP_CAMERA_PREVIEW:
                //    mBackgroundHandler.post(new Runnable() {
                //        @Override public void run() {
                //            CameraController.getInstance().stopCameraPreview();
                //        }
                //    });
                //    break;

                default:
                    break;
            }
        }
    }


}