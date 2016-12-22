package com.alan.alvideo.filter;

import com.alan.alvideo.gles.Texture2dProgram;


public class FilterManager {

    private FilterManager() {
    }

    public static Texture2dProgram getCameraFilter(FilterType filterType) {
        Texture2dProgram.ProgramType programType;
        switch (filterType) {
            case Normal:
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
        }
        Texture2dProgram program = new Texture2dProgram(programType);
        return program;
    }

    public enum FilterType {
        Normal, GRAYSCALE, STARMAKER, SEPIA
    }
}
