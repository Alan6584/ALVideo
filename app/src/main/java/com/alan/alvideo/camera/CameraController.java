package com.alan.alvideo.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;

public class CameraController {

    private static volatile CameraController sInstance;

    private Camera mCamera = null;
    public int mCameraIndex = Camera.CameraInfo.CAMERA_FACING_FRONT;
    public boolean mCameraMirrored = false;

    private final Object mLock = new Object();

    public static CameraController getInstance() {
        if (sInstance == null) {
            synchronized (CameraController.class) {
                if (sInstance == null) {
                    sInstance = new CameraController();
                }
            }
        }
        return sInstance;
    }

    private CameraController() {
    }

    public void setupCamera(SurfaceTexture surfaceTexture) {
        if (mCamera != null) {
            release();
        }

        synchronized (mLock) {
            try {
                if (Camera.getNumberOfCameras() > 0) {
                    mCamera = Camera.open(mCameraIndex);
                } else {
                    mCamera = Camera.open();
                }

                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                Camera.getCameraInfo(mCameraIndex, cameraInfo);

                mCameraMirrored = (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
                mCamera.setDisplayOrientation(90);
                mCamera.setPreviewTexture(surfaceTexture);
            } catch (Exception e) {
                e.printStackTrace();
                mCamera = null;
                e.printStackTrace();
            }

            if (mCamera == null) {
                //Toast.makeText(mContext, "Unable to start camera", Toast.LENGTH_SHORT).showFromSession();
                return;
            }

        }
    }

    public void configureCameraParameters(Camera.Size previewSize) {

        try {
            Camera.Parameters cp = getCameraParameters();
            if (cp == null || mCamera == null) {
                return;
            }
            // 对焦模式
            synchronized (mLock) {
                // 预览尺寸
                if (previewSize != null) {
                    cp.setPreviewSize(previewSize.width, previewSize.height);
                }
                mCamera.setParameters(cp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean startCameraPreview() {
        if (mCamera != null) {
            synchronized (mLock) {
                try {
                    mCamera.startPreview();
                    mCamera.autoFocus(null);
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return false;
    }

    public boolean stopCameraPreview() {

        if (mCamera != null) {
            synchronized (mLock) {
                try {
                    mCamera.stopPreview();
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return false;
    }

    public void release() {
        if (mCamera != null) {
            synchronized (mLock) {
                try {
                    mCamera.setPreviewCallback(null);
                    mCamera.stopPreview();
                    mCamera.release();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    mCamera = null;
                }
            }
        }
    }


    public Camera.Parameters getCameraParameters() {
        if (mCamera != null) {
            synchronized (mLock) {
                try {
                    return mCamera.getParameters();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

}
