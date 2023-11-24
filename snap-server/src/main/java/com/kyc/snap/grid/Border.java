package com.kyc.snap.grid;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Border {

    public static final Border NONE = new Border(-1, 0);

    public int rgb;
    public int width;

    /**
     * Styles are relative (e.g. thin/thick), so this field is null until it is filled by comparing
     * with other borders.
     */
    public Style style = Style.NONE;

    public Border(@JsonProperty("rgb") int rgb, @JsonProperty("width") int width) {
        this.rgb = rgb;
        this.width = width;
    }

    public enum Style {
        NONE,
        THIN,
        MEDIUM,
        THICK,
    }
}
