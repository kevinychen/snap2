package com.kyc.snap.image;

import java.awt.Point;
import java.util.Set;

import lombok.Data;

@Data
public class ImageBlob {

    private final int x;
    private final int y;
    private final int width;
    private final int height;

    // Points right outside the boundary of this blob, but not part of the blob.
    private final Set<Point> fencePoints;

    // A point that when flood-filled inside the fence points, gives the entire blob.
    private final Point innerPoint;
}
