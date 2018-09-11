package com.kyc.snap.grid;

import lombok.Data;

@Data
public class Border {

    private final int rgb;
    private final int width;

    /**
     * Styles are relative (e.g. thin/thick), so this field is null until it is filled by comparing
     * with other borders.
     */
    private Style style;

    public static enum Style {
        NONE,
        THIN,
        MEDIUM,
        THICK,
    }
}
