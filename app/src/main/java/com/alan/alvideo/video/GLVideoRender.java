package com.alan.alvideo.video;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.alan.alvideo.gles.GLUtil;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Author: wangjianjun.
 * Date: 17/1/9 12:14.
 * Mail: alanwang6584@gmail.com
 */

public class GLVideoRender implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {

    public interface SurfaceListener{
        void onPrepared();
    }

    private static final String vertexShaderCode =
            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTextureCoord;\n" +
            "uniform mat4 uMVPMatrix;\n" +
            "uniform mat4 uTexMatrix;\n" +
            "varying vec2 vTextureCoord;\n" +
            "void main() {\n" +
            "   gl_Position = uMVPMatrix * aPosition;\n" +
            "   vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
            "}\n";

//    private static final String fragmentShaderCode =
//            "#extension GL_OES_EGL_image_external : require\n" +
//                    "precision mediump float;\n" +
//                    "varying vec2 vTextureCoord;\n" +
//                    "uniform samplerExternalOES uTexture;\n" +
//                    "void main() {\n" +
//                    "   gl_FragColor = texture2D(uTexture, vTextureCoord);\n" +
//                    "}\n";

    private static final String fragmentShaderCode =
            "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 vTextureCoord;\n" +
            "uniform samplerExternalOES uTexture;\n" +
            "void main() {\n" +
            "   vec4 sp;\n" +
            "   if(vTextureCoord.y < 0.5) {\n" +
            "       sp = texture2D(uTexture, vec2(vTextureCoord.x, vTextureCoord.y + 0.25));\n" +
            "   } else {\n" +
            "       sp = texture2D(uTexture, vec2(vTextureCoord.x, (vTextureCoord.y - 0.25)));\n" +
            "   }\n" +
            "   gl_FragColor = sp;\n" +
            "}\n";

    private static float vertexCoords[] = {
            -1.0f, 1.0f,   // top left
            -1.0f, -1.0f,   // bottom left
            1.0f, -1.0f,    // bottom right
            1.0f, 1.0f}; // top right

//    private float textureCoords[] = {
//            0.0f, 0.0f,
//            0.0f, 1.0f,
//            1.0f, 1.0f,
//            1.0f, 0.0f};

    private float textureCoords[] = {
            0.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f,
            1.0f, 1.0f};

    private static short drawOrderArr[] = {0, 1, 2, 0, 2, 3};

    private FloatBuffer vertexBuffer;
    private FloatBuffer textureBuffer;
    private ShortBuffer drawOrderBuffer;
    private int textureID;

    protected int mProgram;
    protected int mPositionHandle;
    protected int mTextureCoordHandler;
    protected int mTextureHandle;
    protected int mMVPMatrixHandle;
    protected int mTexMatrixHandle;
    private float[] mvpMatrix = new float[16];
    private float[] texMatrix = new float[16];
    private boolean frameAvailable = false;

    private SurfaceTexture videoTexture;
    private SurfaceListener mSurfaceListener;

    public GLVideoRender() {

    }

    public void setmSurfaceListener(SurfaceListener mSurfaceListener) {
        this.mSurfaceListener = mSurfaceListener;
    }

    public SurfaceTexture getVideoTexture(){
        return videoTexture;
    }

    /**
     * 初始化顶点坐标和纹理坐标
     */
    private void initCoodsBuffer(){
        vertexBuffer = GLUtil.createFloatBuffer(vertexCoords);
        textureBuffer = GLUtil.createFloatBuffer(textureCoords);
        drawOrderBuffer = GLUtil.creatShortBuffer(drawOrderArr);
        android.opengl.Matrix.setIdentityM(mvpMatrix, 0);
    }

    /**
     * 初始化OpenGL
     */
    private void initShaders(){
        mProgram = GLUtil.createProgram(vertexShaderCode, fragmentShaderCode);
        GLUtil.checkGlError("createProgram");
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");// get handle to vertex shader's aPosition member
        mTextureCoordHandler = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");// get handle to vertex shader's aPosition member
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");// get handle to shape's transformation matrix
        mTexMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uTexMatrix");// get handle to shape's transformation matrix
        mTextureHandle = GLES20.glGetUniformLocation(mProgram, "uTexture");// get handle to shape's uTexture
        GLUtil.checkGlError("mTextureHandle");
    }

    /**
     * 初始化纹理
     */
    private void initTexture(){
        int[] textures = new int[1];
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glGenTextures(1, textures, 0);
        GLUtil.checkGlError("Texture generate");
        textureID = textures[0];
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureID);
        GLUtil.checkGlError("Texture bind");
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,GLES20. GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        videoTexture = new SurfaceTexture(textureID);
        videoTexture.setOnFrameAvailableListener(this);
    }

    public void setMvpMatrix(float[] mvpMatrix) {
        this.mvpMatrix = mvpMatrix;
    }

    public void draw() {
        GLES20.glUseProgram(mProgram);
        GLES20.glVertexAttribPointer(mPositionHandle, 2, GLES20.GL_FLOAT, false, 2 * 4, vertexBuffer);
        GLES20.glVertexAttribPointer(mTextureCoordHandler, 2, GLES20.GL_FLOAT, false, 2 * 4, textureBuffer);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureID);
        GLES20.glUniform1i(mTextureHandle, 0);

        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0);// Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mTexMatrixHandle, 1, false, texMatrix, 0);// Apply the projection and view transformation


        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glEnableVertexAttribArray(mTextureCoordHandler);

//        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vCount);
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, drawOrderArr.length, GLES20.GL_UNSIGNED_SHORT, drawOrderBuffer);

        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTextureCoordHandler);
    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        initCoodsBuffer();
        initShaders();
        initTexture();
    }

    private int width;
    private int height;
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.width = width;
        this.height = height;
        Log.e("Alan", "width = " + width + "--->height = " + height);
        if (mSurfaceListener != null){
            mSurfaceListener.onPrepared();
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        synchronized (this) {
            if (frameAvailable) {
                videoTexture.updateTexImage();
                videoTexture.getTransformMatrix(texMatrix);
                frameAvailable = false;
            }
        }
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glViewport(0, 0, width, height);
        this.draw();
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        synchronized (this) {
            frameAvailable = true;
        }
    }
}
