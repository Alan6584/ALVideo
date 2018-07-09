package com.alan.alvideo.camera;

import android.hardware.Camera;
import android.util.Log;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by wangjianjun on 16/12/22.
 * alanwang6584@gmail.com
 */

public class CameraUtils {

    public static final int DEFAULT_PREVIEW_HEIGHT = 480;
    public static final int DEFAULT_PREVIEW_WIDTH = 640;
    private final float DEFAULT_CAMERA_RATIO = 4f / 3f;

    /**
     * 根据需求获取cameraID,如果是前置摄像头则获取前置摄像头的ID，如果是后置摄像头则获取后置摄像头ID
     * 后面打开摄像头需要用到该ID
     * @param isFrontCamera 是否是前置摄像头
     * @return 返回需要的摄像头ID，如果没有摄像头或获取不到则返回-1
     */
    public static int getTheCameraId(boolean isFrontCamera){
        int frontCameraId = -1;
        int targetFacing = isFrontCamera ? Camera.CameraInfo.CAMERA_FACING_FRONT
                : Camera.CameraInfo.CAMERA_FACING_BACK;
        int numCameras = Camera.getNumberOfCameras();
        for (int cameraId = 0; cameraId < numCameras; cameraId++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, info);
            if (info.facing == targetFacing) {
                frontCameraId = cameraId;
                break;
            }
        }
        return frontCameraId;
    }

    /**
     * 获取最优的预览尺寸,具体实现思路是：根据默认的预览比例（如4：3）从相机支持的尺寸中查找最接近于想要设置的预览尺寸。
     * @param parameters
     * @param targetWidth
     * @param targetHeight
     * @return
     */
    public static Camera.Size getOptimalPreviewSize(Camera.Parameters parameters, int targetWidth, int targetHeight){
        Log.e("Alan", "targetWidth = " + targetWidth + "--->>>targetHeight = " + targetHeight + "--->>> targetHeight" + targetHeight);

        Camera.Size optimalSize = null;
        if (parameters == null || targetWidth <= 0 || targetHeight <= 0) {
            return null;
        }
        //获取当前支持的预览尺寸列表
        List<Camera.Size> supportSizeList = parameters.getSupportedPreviewSizes();
        if (supportSizeList == null) {
            return null;
        }
        //将支持的预览尺寸列表按从小到大排序，目的是为了获取较大最接近于目标尺寸的预览尺寸
        Collections.sort(supportSizeList, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size lhs, Camera.Size rhs) {
                return lhs.width - rhs.width;
            }
        });

        final float RATIO_TOLERANCE = 0.05f;//尺寸宽高比容差，预览尺寸并不完全满足4：3
        float targetRatio = (float) targetWidth / targetHeight;
        float minDiff = Float.MAX_VALUE;//最接近于目标尺寸的比例

        for (Camera.Size size : supportSizeList) {
            float ratio = (float) size.width / size.height;
            Log.d("Alan", "size.width = " + size.width + "--->>>size.height = " + size.height + "--->>> ratio = " + ratio);
            if (Math.abs(ratio - targetRatio) > RATIO_TOLERANCE) {
                continue;
            }

            if (optimalSize != null && size.height > targetHeight) {
                break;
            }

            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Float.MAX_VALUE;
            for (Camera.Size size : supportSizeList) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    /**
     * 获取合适的帧率
     * @param expectedFps 期望的帧率
     * @param fpsRanges
     * @return
     */
    public static int[] getSuitablePreviewFps(int expectedFps, List<int[]> fpsRanges) {
        expectedFps *= 1000;
        int[] retRange;
        if (fpsRanges != null && fpsRanges.size() > 0) {
            retRange = fpsRanges.get(0);
            int minDiff = Integer.MAX_VALUE;
            for (int[] range : fpsRanges) {
                if (range[0] <= expectedFps && range[1] >= expectedFps) {
                    int curDiff = Math.abs(range[0] - expectedFps) + Math.abs(range[1] - expectedFps);
                    if (curDiff < minDiff) {
                        minDiff = curDiff;
                        retRange = range;
                    }
                }
            }
        } else {
            retRange = new int[]{expectedFps, expectedFps};
        }
        return retRange;
    }
}
