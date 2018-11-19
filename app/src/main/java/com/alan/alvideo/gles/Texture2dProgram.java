/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alan.alvideo.gles;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import java.nio.FloatBuffer;

/**
 * GL program and supporting functions for textured 2D shapes.
 */
public class Texture2dProgram {
    private static final String TAG = Texture2dProgram.class.getSimpleName();

    public enum ProgramType {
        TEXTURE_2D, TEXTURE_EXT, TEXTURE_EXT_BW, TEXTURE_EXT_FILT, TEXTURE_EXT_PURPLE, TEXTURE_EXT_SEPIA, TEXTURE_EXT_BEAUTY
    }

    // Simple vertex shader, used for all programs.
    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uTexMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * aPosition;\n" +
                    "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
                    "}\n";

    // Simple fragment shader for use with "normal" 2D textures.
    private static final String FRAGMENT_SHADER_2D =
            "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform sampler2D sTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "    gl_FragColor *= " + 1.0f + ";\n" +
                    "}\n";

    // Simple fragment shader for use with external 2D textures (e.g. what we get from
    // SurfaceTexture).
    private static final String FRAGMENT_SHADER_EXT =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";

    // Fragment shader that converts color to black & white with a simple transformation.
    private static final String FRAGMENT_SHADER_EXT_BW =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
                    "    float color = tc.r * 0.3 + tc.g * 0.59 + tc.b * 0.11;\n" +
                    "    gl_FragColor = vec4(color, color, color, 1.0);\n" +
                    "}\n";

    private static final String FRAGMENT_SHADER_EXT_SEPIA =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
                    "    float color = (tc.r + tc.g + tc.b) / 256.0;\n" +
                    "    gl_FragColor = vec4(color * 130.0, color * 80.0, color * 50.0, 1.0);\n" +
                    "}\n";

    private static final String FRAGMENT_SHADER_EXT_PURPLE =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "    vec4 tc = texture2D(sTexture, vTextureCoord);\n" +
                    "    float colorR = tc.r * 1.0 + tc.g * 0.25 + tc.b * 0.25;\n" +
                    "    float colorG = tc.r * 0.25 + tc.g * 0.25 + tc.b * 0.25;\n" +
                    "    float colorB = tc.r * 0.25 + tc.g * 0.25 + tc.b * 1.5;\n" +
                    "    gl_FragColor = vec4(colorR, colorG, colorB, 1.0);\n" +
                    "}\n";

    // Fragment shader with a convolution filter.  The upper-left half will be drawn normally,
    // the lower-right half will have the filter applied, and a thin red line will be drawn
    // at the border.
    //
    // This is not optimized for performance.  Some things that might make this faster:
    // - Remove the conditionals.  They're used to present a half & half view with a red
    //   stripe across the middle, but that's only useful for a demo.
    // - Unroll the loop.  Ideally the compiler does this for you when it's beneficial.
    // - Bake the filter kernel into the shader, instead of passing it through a uniform
    //   array.  That, combined with loop unrolling, should reduce memory accesses.
    public static final int KERNEL_SIZE = 9;
    private static final String FRAGMENT_SHADER_EXT_FILT =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "#define KERNEL_SIZE " + KERNEL_SIZE + "\n" +
                    "precision highp float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "uniform float uKernel[KERNEL_SIZE];\n" +
                    "uniform vec2 uTexOffset[KERNEL_SIZE];\n" +
                    "uniform float uColorAdjust;\n" +
                    "void main() {\n" +
                    "    int i = 0;\n" +
                    "    vec4 sum = vec4(0.0);\n" +
                    "    for (i = 0; i < KERNEL_SIZE; i++) {\n" +
                    "        vec4 texc = texture2D(sTexture, vTextureCoord + uTexOffset[i]);\n" +
                    "        sum += texc * uKernel[i];\n" +
                    "    }\n" +
                    "    sum += uColorAdjust;\n" +
                    "    gl_FragColor = sum;\n" +
                    "}\n";

    private static final String FRAGMENT_SHADER_EXT_BEAUTY =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +

                    "uniform samplerExternalOES sTexture;\n" +
                    "uniform vec4 levelParam;\n" +
                    "varying mediump vec2 vTextureCoord;\n" +

                    "void main() {\n" +
                    "vec3 centralColor;\n" +
                    "float sampleColor;\n" +
                    "vec2 blurCoordinates[20];\n" +
                    "float mul = 2.0;\n" +
                    "float mul_x = mul / 480.0;\n" +
                    "float mul_y = mul / 640.0;\n" +

                    "blurCoordinates[0] = vTextureCoord + vec2(0.0 * mul_x,-10.0 * mul_y);\n" +
                    "blurCoordinates[1] = vTextureCoord + vec2(5.0 * mul_x,-8.0 * mul_y);\n" +
                    "blurCoordinates[2] = vTextureCoord + vec2(8.0 * mul_x,-5.0 * mul_y);\n" +
                    "blurCoordinates[3] = vTextureCoord + vec2(10.0 * mul_x,0.0 * mul_y);\n" +
                    "blurCoordinates[4] = vTextureCoord + vec2(8.0 * mul_x,5.0 * mul_y);\n" +
                    "blurCoordinates[5] = vTextureCoord + vec2(5.0 * mul_x,8.0 * mul_y);\n" +
                    "blurCoordinates[6] = vTextureCoord + vec2(0.0 * mul_x,10.0 * mul_y);\n" +
                    "blurCoordinates[7] = vTextureCoord + vec2(-5.0 * mul_x,8.0 * mul_y);\n" +
                    "blurCoordinates[8] = vTextureCoord + vec2(-8.0 * mul_x,5.0 * mul_y);\n" +
                    "blurCoordinates[9] = vTextureCoord + vec2(-10.0 * mul_x,0.0 * mul_y);\n" +
                    "blurCoordinates[10] = vTextureCoord + vec2(-8.0 * mul_x,-5.0 * mul_y);\n" +
                    "blurCoordinates[11] = vTextureCoord + vec2(-5.0 * mul_x,-8.0 * mul_y);\n" +
                    "blurCoordinates[12] = vTextureCoord + vec2(0.0 * mul_x,-6.0 * mul_y);\n" +
                    "blurCoordinates[13] = vTextureCoord + vec2(-4.0 * mul_x,-4.0 * mul_y);\n" +
                    "blurCoordinates[14] = vTextureCoord + vec2(-6.0 * mul_x,0.0 * mul_y);\n" +
                    "blurCoordinates[15] = vTextureCoord + vec2(-4.0 * mul_x,4.0 * mul_y);\n" +
                    "blurCoordinates[16] = vTextureCoord + vec2(0.0 * mul_x,6.0 * mul_y);\n" +
                    "blurCoordinates[17] = vTextureCoord + vec2(4.0 * mul_x,4.0 * mul_y);\n" +
                    "blurCoordinates[18] = vTextureCoord + vec2(6.0 * mul_x,0.0 * mul_y);\n" +
                    "blurCoordinates[19] = vTextureCoord + vec2(4.0 * mul_x,-4.0 * mul_y);\n" +

                    "centralColor = texture2D(sTexture, vTextureCoord).rgb;\n" +
                    "sampleColor = centralColor.g * 22.0;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[0]).g;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[1]).g;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[2]).g;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[3]).g;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[4]).g;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[5]).g;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[6]).g;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[7]).g;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[8]).g;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[9]).g;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[10]).g;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[11]).g;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[12]).g * 2.0;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[13]).g * 2.0;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[14]).g * 2.0;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[15]).g * 2.0;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[16]).g * 2.0;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[17]).g * 2.0;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[18]).g * 2.0;\n" +
                    "sampleColor += texture2D(sTexture, blurCoordinates[19]).g * 2.0;\n" +
                    "sampleColor = sampleColor/50.0;\n" +

                    "float dis = centralColor.g - sampleColor + 0.5;\n" +
                    "if (dis <= 0.5) {\n" +
                    "dis = dis * dis * 2.0;\n" +
                    "} else {\n" +
                    "dis = 1.0 - ((1.0 - dis)*(1.0 - dis) * 2.0);\n" +
                    "}\n" +

                    "if (dis <= 0.5) {\n" +
                    "dis = dis * dis * 2.0;\n" +
                    "} else {\n" +
                    "dis = 1.0 - ((1.0 - dis)*(1.0 - dis) * 2.0);\n" +
                    "}\n" +

                    "if (dis <= 0.5) {\n" +
                    "dis = dis * dis * 2.0;\n" +
                    "} else {\n" +
                    "dis = 1.0 - ((1.0 - dis)*(1.0 - dis) * 2.0);\n" +
                    "}\n" +

                    "if (dis <= 0.5) {\n" +
                    "dis = dis * dis * 2.0;\n" +
                    "} else {\n" +
                    "dis = 1.0 - ((1.0 - dis)*(1.0 - dis) * 2.0);\n" +
                    "}\n" +

                    "if (dis <= 0.5) {\n" +
                    "dis = dis * dis * 2.0;\n" +
                    "} else {\n" +
                    "dis = 1.0 - ((1.0 - dis)*(1.0 - dis) * 2.0);\n" +
                    "}\n" +

                    "float aa = 1.03;\n" +
                    "vec3 smoothColor = centralColor*aa - vec3(dis)*(aa-1.0);\n" +

                    "float hue = dot(smoothColor, vec3(0.299,0.587,0.114));\n" +

                    "float huePow = pow(hue, levelParam.x);\n" +
                    "aa = 1.0 + huePow*0.1;\n" +
                    "smoothColor = centralColor*aa - vec3(dis)*(aa-1.0);\n" +

                    "smoothColor.r = clamp(pow(smoothColor.r, levelParam.y),0.0,1.0);\n" +
                    "smoothColor.g = clamp(pow(smoothColor.g, levelParam.y),0.0,1.0);\n" +
                    "smoothColor.b = clamp(pow(smoothColor.b, levelParam.y),0.0,1.0);\n" +

                    "vec3 lvse = vec3(1.0)-(vec3(1.0)-smoothColor)*(vec3(1.0)-centralColor);\n" +
                    "vec3 bianliang = max(smoothColor, centralColor);\n" +
                    "vec3 temp = 2.0*centralColor*smoothColor;\n" +
                    "vec3 rouguang = temp + centralColor*centralColor - temp*centralColor;\n" +

                    "gl_FragColor = vec4(mix(centralColor, lvse, huePow), 1.0);\n" +
                    "gl_FragColor.rgb = mix(gl_FragColor.rgb, bianliang, huePow);\n" +
                    "gl_FragColor.rgb = mix(gl_FragColor.rgb, rouguang, levelParam.z);\n" +

                    "mat3 saturateMatrix = mat3(1.1102, -0.0598, -0.061, -0.0774, 1.0826, -0.1186, -0.0228, -0.0228, 1.1772);\n" +
                    "vec3 satcolor = gl_FragColor.rgb * saturateMatrix;\n" +
                    "gl_FragColor.rgb = mix(gl_FragColor.rgb, satcolor, levelParam.w);\n" +
                    "}\n";

    private ProgramType mProgramType;

    // Handles to the GL program and various components of it.
    private int mProgramHandle;
    private int muMVPMatrixLoc;
    private int muTexMatrixLoc;
    private int muKernelLoc;
    private int muTexOffsetLoc;
    private int muColorAdjustLoc;
    private int maPositionLoc;
    private int maTextureCoordLoc;
    private int mGLLevelParamLocation;
    private float[] mBeautyLevelParam;

    private int mTextureTarget;

    private float[] mKernel = new float[KERNEL_SIZE];
    private float[] mTexOffset;
    private float mColorAdjust;


    /**
     * Prepares the program in the current EGL context.
     */
    public Texture2dProgram(ProgramType programType) {
        mProgramType = programType;

        switch (programType) {
            case TEXTURE_2D:
                mTextureTarget = GLES20.GL_TEXTURE_2D;
                mProgramHandle = GLUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_2D);
                break;
            case TEXTURE_EXT:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle = GLUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT);
                break;
            case TEXTURE_EXT_BW:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle = GLUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_BW);
                break;
            case TEXTURE_EXT_PURPLE:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle = GLUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_PURPLE);
                break;
            case TEXTURE_EXT_SEPIA:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle = GLUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_SEPIA);
                break;
            case TEXTURE_EXT_FILT:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle = GLUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_FILT);
                break;
            case TEXTURE_EXT_BEAUTY:
                mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
                mProgramHandle = GLUtil.createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT_BEAUTY);
                mGLLevelParamLocation = GLES20.glGetUniformLocation(mProgramHandle, "levelParam");
                mBeautyLevelParam = setLevel(3);
                break;
            default:
                throw new RuntimeException("Unhandled type " + programType);
        }
        if (mProgramHandle == 0) {
            throw new RuntimeException("Unable to create program");
        }
        Log.d(TAG, "Created program " + mProgramHandle + " (" + programType + ")");

        // get locations of attributes and uniforms

        maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        GLUtil.checkLocation(maPositionLoc, "aPosition");
        maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
        GLUtil.checkLocation(maTextureCoordLoc, "aTextureCoord");
        muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        GLUtil.checkLocation(muMVPMatrixLoc, "uMVPMatrix");
        muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
        GLUtil.checkLocation(muTexMatrixLoc, "uTexMatrix");
        muKernelLoc = GLES20.glGetUniformLocation(mProgramHandle, "uKernel");
        if (muKernelLoc < 0) {
            // no kernel in this one
            muKernelLoc = -1;
            muTexOffsetLoc = -1;
            muColorAdjustLoc = -1;
        } else {
            // has kernel, must also have tex offset and color adj
            muTexOffsetLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexOffset");
            GLUtil.checkLocation(muTexOffsetLoc, "uTexOffset");
            muColorAdjustLoc = GLES20.glGetUniformLocation(mProgramHandle, "uColorAdjust");
            GLUtil.checkLocation(muColorAdjustLoc, "uColorAdjust");

            // initialize default values
            setKernel(new float[]{0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f}, 0f);
            setTexSize(256, 256);
        }
    }

    /**
     * Releases the program.
     * <p>
     * The appropriate EGL context must be current (i.e. the one that was used to create
     * the program).
     */
    public void release() {
        Log.d(TAG, "deleting program " + mProgramHandle);
        GLES20.glDeleteProgram(mProgramHandle);
        mProgramHandle = -1;
    }

    /**
     * Returns the program type.
     */
    public ProgramType getProgramType() {
        return mProgramType;
    }

    /**
     * Creates a texture object suitable for use with this program.
     * <p>
     * On exit, the texture will be bound.
     */
    public int createTextureObject() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLUtil.checkGlError("glGenTextures");

        int texId = textures[0];
        GLES20.glBindTexture(mTextureTarget, texId);
        GLUtil.checkGlError("glBindTexture " + texId);

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        GLUtil.checkGlError("glTexParameter");

        return texId;
    }

    /**
     * Configures the convolution filter values.
     *
     * @param values Normalized filter values; must be KERNEL_SIZE elements.
     */
    public void setKernel(float[] values, float colorAdj) {
        if (values.length != KERNEL_SIZE) {
            throw new IllegalArgumentException("Kernel size is " + values.length +
                    " vs. " + KERNEL_SIZE);
        }
        System.arraycopy(values, 0, mKernel, 0, KERNEL_SIZE);
        mColorAdjust = colorAdj;
        //Log.d(TAG, "filt kernel: " + Arrays.toString(mKernel) + ", adj=" + colorAdj);
    }

    /**
     * Sets the size of the texture.  This is used to find adjacent texels when filtering.
     */
    public void setTexSize(int width, int height) {
        float rw = 1.0f / width;
        float rh = 1.0f / height;

        // Don't need to create a new array here, but it's syntactically convenient.
        mTexOffset = new float[]{
                -rw, -rh, 0f, -rh, rw, -rh,
                -rw, 0f, 0f, 0f, rw, 0f,
                -rw, rh, 0f, rh, rw, rh
        };
        //Log.d(TAG, "filt size: " + width + "x" + height + ": " + Arrays.toString(mTexOffset));
    }

    /**
     * Issues the draw call.  Does the full setup on every call.
     *
     * @param mvpMatrix       The 4x4 projection matrix.
     * @param vertexBuffer    Buffer with vertex position data.
     * @param firstVertex     Index of first vertex to use in vertexBuffer.
     * @param vertexCount     Number of vertices in vertexBuffer.
     * @param coordsPerVertex The number of coordinates per vertex (e.g. x,y is 2).
     * @param vertexStride    Width, in bytes, of the position data for each vertex (often
     *                        vertexCount * sizeof(float)).
     * @param texMatrix       A 4x4 transformation matrix for texture coords.  (Primarily intended
     *                        for use with SurfaceTexture.)
     * @param texBuffer       Buffer with vertex texture data.
     * @param texStride       Width, in bytes, of the texture data for each vertex.
     */
    public void draw(float[] mvpMatrix, FloatBuffer vertexBuffer, int firstVertex,
                     int vertexCount, int coordsPerVertex, int vertexStride,
                     float[] texMatrix, FloatBuffer texBuffer, int textureId, int texStride) {
        GLUtil.checkGlError("draw start");

        // Select the program.
        GLES20.glUseProgram(mProgramHandle);
        GLUtil.checkGlError("glUseProgram");

        // Set the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(mTextureTarget, textureId);

        // Copy the model / view / projection matrix over.
        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0);
        GLUtil.checkGlError("glUniformMatrix4fv");

        // Copy the texture transformation matrix over.
        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, 0);
        GLUtil.checkGlError("glUniformMatrix4fv");

        // Enable the "aPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(maPositionLoc);
        GLUtil.checkGlError("glEnableVertexAttribArray");

        // Connect vertexBuffer to "aPosition".
        GLES20.glVertexAttribPointer(maPositionLoc, coordsPerVertex,
                GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);
        GLUtil.checkGlError("glVertexAttribPointer");

        // Enable the "aTextureCoord" vertex attribute.
        GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
        GLUtil.checkGlError("glEnableVertexAttribArray");

        // Connect texBuffer to "aTextureCoord".
        GLES20.glVertexAttribPointer(maTextureCoordLoc, 2,
                GLES20.GL_FLOAT, false, texStride, texBuffer);
        GLUtil.checkGlError("glVertexAttribPointer");

        // Populate the convolution kernel, if present.
        if (muKernelLoc >= 0) {
            GLES20.glUniform1fv(muKernelLoc, KERNEL_SIZE, mKernel, 0);
            GLES20.glUniform2fv(muTexOffsetLoc, KERNEL_SIZE, mTexOffset, 0);
            GLES20.glUniform1f(muColorAdjustLoc, mColorAdjust);
        }

        if (mProgramType == ProgramType.TEXTURE_EXT_BEAUTY) {
            GLES20.glUniform4fv(mGLLevelParamLocation, 1, FloatBuffer.wrap(mBeautyLevelParam));
        }

        // Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, firstVertex, vertexCount);
        GLUtil.checkGlError("glDrawArrays");

        // Done -- disable vertex array, texture, and program.
        GLES20.glDisableVertexAttribArray(maPositionLoc);
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
        GLES20.glBindTexture(mTextureTarget, 0);
        GLES20.glUseProgram(0);
    }

    /**
     * 设置美颜级别
     * @param _beautyLevel
     * @return
     */
    private float[] setLevel(int _beautyLevel) {
        float hue_,smoothColor_,rouguang_,saturate_;
        switch (_beautyLevel) {

            case 5:
            {
                hue_ = 0.33f;
                smoothColor_ = 0.63f;
                rouguang_ = 0.4f;
                saturate_ = 0.35f;
                break;
            }
            case 4:
            {
                hue_ = 0.4f;
                smoothColor_ = 0.7f;
                rouguang_ = 0.38f;
                saturate_ = 0.3f;
                break;
            }
            case 3:
            {
                hue_ = 0.6f;
                smoothColor_ = 0.8f;
                rouguang_ = 0.25f;
                saturate_ = 0.25f;
                break;
            }
            case 2:
            {
                hue_ = 0.8f;
                smoothColor_ = 0.9f;
                rouguang_ = 0.2f;
                saturate_ = 0.2f;
                break;
            }
            default:
            {
                hue_ = 1.0f;
                smoothColor_ = 1.0f;
                rouguang_ = 0.15f;
                saturate_ = 0.15f;
                break;
            }
        }
        float levelParam[] = {hue_,smoothColor_,rouguang_,saturate_};
        return levelParam;
    }
}
