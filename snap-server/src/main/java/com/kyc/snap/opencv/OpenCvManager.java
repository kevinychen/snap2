
package com.kyc.snap.opencv;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import lombok.Data;
import nu.pattern.OpenCV;

public class OpenCvManager {

    public static final double DEFAULT_CANNY_THRESHOLD_1 = 60;
    public static final double DEFAULT_CANNY_THRESHOLD_2 = 180;
    public static final int DEFAULT_HOUGH_THRESHOLD = 32;

    public OpenCvManager() {
        OpenCV.loadShared();
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    /**
     * Find all lines in the image that are at least the minimum line length and are at orientations
     * that are multiples of theta, e.g. theta = PI / 2 returns all cardinal lines (horizontal and
     * vertical).
     */
    public List<Line> findLines(BufferedImage image, double theta, double minLineLength) {
        Mat mat = toMat(image);
        Mat edges = canny(mat, DEFAULT_CANNY_THRESHOLD_1, DEFAULT_CANNY_THRESHOLD_2);
        Mat lines = hough(edges, theta, DEFAULT_HOUGH_THRESHOLD, minLineLength);

        List<Line> finalLines = new ArrayList<>();
        for (int i = 0; i < lines.rows(); i++) {
            double[] data = lines.get(i, 0);
            finalLines.add(new Line(data[0], data[1], data[2], data[3]));
        }
        return finalLines;
    }

    private Mat toMat(BufferedImage image) {
        BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        for (int x = 0; x < image.getWidth(); x++)
            for (int y = 0; y < image.getHeight(); y++)
                newImage.setRGB(x, y, image.getRGB(x, y));
        Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
        byte[] data = ((DataBufferByte) newImage.getRaster().getDataBuffer()).getData();
        mat.put(0, 0, data);
        return mat;
    }

    private Mat canny(Mat image, double threshold1, double threshold2) {
        Mat edges = new Mat();
        Imgproc.Canny(image, edges, threshold1, threshold2);
        return edges;
    }

    private Mat hough(Mat edges, double theta, int threshold, double minLineLength) {
        Mat lines = new Mat();
        Imgproc.HoughLinesP(edges, lines, 1, theta, threshold, minLineLength, 0);
        return lines;
    }

    @Data
    public static class Line {

        private final double x1;
        private final double y1;
        private final double x2;
        private final double y2;
    }
}
