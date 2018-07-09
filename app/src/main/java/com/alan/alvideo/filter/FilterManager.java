package com.alan.alvideo.filter;

import com.alan.alvideo.gles.Texture2dProgram;

/**
 * Created by wangjianjun on 16/12/22.
 * alanwang6584@gmail.com
 */
public class FilterManager {

    /**
     * 滤镜类型
     */
    public enum FilterType {
        NORMAL, GRAYSCALE, STARMAKER, SEPIA, BEAUTY
    }

    private FilterManager() {
    }

    /**
     * 根据滤镜类型返回纹理渲染器
     *
     * @param filterType
     * @return
     */
    public static Texture2dProgram getCameraFilter(FilterType filterType) {
        Texture2dProgram.ProgramType programType;
        switch (filterType) {
            case NORMAL:
            default:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT;
                break;
            case GRAYSCALE:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_BW;
                break;
            case STARMAKER:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_STARMAKER;
                break;
            case SEPIA:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_SEPIA;
                break;
            case BEAUTY:
                programType = Texture2dProgram.ProgramType.TEXTURE_EXT_BEAUTY;
                break;
        }
        Texture2dProgram program = new Texture2dProgram(programType);
        return program;
    }
}
