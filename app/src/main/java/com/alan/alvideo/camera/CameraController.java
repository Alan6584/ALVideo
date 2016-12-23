package com.alan.alvideo.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;

/**
 * Created by wangjianjun on 16/12/22.
 * alanwang6584@gmail.com
 */
public class CameraController {

    private static volatile CameraController _instance;
    private Camera mCamera = null;
    private boolean isFrontCamera = true;
    private final Object mLock = new Object();

    /**
     * 获取相机控制器的单例，由该类统一控制管理Camera
     * @return
     */
    public static CameraController getInstance() {
        if (_instance == null) {
            synchronized (CameraController.class) {
                if (_instance == null) {
                    _instance = new CameraController();
                }
            }
        }
        return _instance;
    }

    /**
     * 禁止外部直接初始化
     */
    private CameraController() {}

    public void setupCamera(SurfaceTexture surfaceTexture) {
        if (mCamera != null) {
            release();
        }
        synchronized (mLock) {
            try {
                //获取前置摄像头的cameraId,并打开相机
                int cameraId = CameraUtils.getTheCameraId(isFrontCamera);
                mCamera = Camera.open(cameraId);

                //TODO 此处需要做适配，某些机型需要旋转270，而且横屏和竖屏旋转角度也不一样
                mCamera.setDisplayOrientation(90);//设置相机预览方向
                mCamera.setPreviewTexture(surfaceTexture);//绑定预览纹理
            } catch (Exception e) {
                e.printStackTrace();
                mCamera = null;
                e.printStackTrace();
            }

            //TODO 相机打开失败需要做个回调
            if (mCamera == null) {
                return;
            }

        }
    }

    /**
     * 设置相机参数
     * @param previewSize
     */
    public void configureCameraParameters(Camera.Size previewSize) {
        try {
            Camera.Parameters parameters = getCameraParameters();
            if (parameters == null || mCamera == null) {
                return;
            }
            synchronized (mLock) {
                // 设置预览尺寸
                if (previewSize != null) {
                    parameters.setPreviewSize(previewSize.width, previewSize.height);
                }
                mCamera.setParameters(parameters);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 开始预览
     * @return
     */
    public boolean startCameraPreview() {
        if (mCamera != null) {
            synchronized (mLock) {
                try {
                    mCamera.startPreview();
                    mCamera.autoFocus(null);//设置自动对焦
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    /**
     * 停止预览
     * @return
     */
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

    /**
     * 释放相机资源
     */
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


    /**
     * 获取相机参数信息
     * @return
     */
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
