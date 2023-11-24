package com.kyc.snap.image;

import java.awt.Point;
import java.util.Set;

/**
 * @param fencePoints Points right outside the boundary of this blob, but not part of the blob.
 * @param innerPoint  A point that when flood-filled inside the fence points, gives the entire blob.
 */
public record ImageBlob(int x, int y, int width, int height, Set<Point> fencePoints, Point innerPoint) {}
