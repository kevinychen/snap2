package com.kyc.snap.image;

import java.awt.Point;
import java.util.List;

import lombok.Data;

@Data
public class ImageBlob {

    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final List<Point> points;
}