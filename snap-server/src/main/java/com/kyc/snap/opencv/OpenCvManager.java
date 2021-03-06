
package com.kyc.snap.opencv;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.lept;
import org.bytedeco.javacpp.lept.PIX;
import org.bytedeco.javacpp.opencv_java;
import org.bytedeco.javacpp.tesseract.TessBaseAPI;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.TermCriteria;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.kyc.snap.image.ImageUtils;

import lombok.Data;

public class OpenCvManager {

    public static final double DEFAULT_CANNY_THRESHOLD_1 = 60;
    public static final double DEFAULT_CANNY_THRESHOLD_2 = 180;
    public static final int DEFAULT_HOUGH_THRESHOLD = 32;
    public static final int DEFAULT_KMEANS_MAX_ITER = 100;

    public OpenCvManager() {
        Loader.load(opencv_java.class);
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

    public Clusters findClusters(List<Tuple> tuples, int numClusters) {
        if (tuples.isEmpty())
            return new Clusters(0, ImmutableMap.of(), ImmutableList.of());

        int numDimensions = tuples.get(0).size();

        Mat data = new Mat(tuples.size(), numDimensions, CvType.CV_32FC1);
        for (int i = 0; i < tuples.size(); i++)
            for (int j = 0; j < numDimensions; j++)
                data.put(i, j, tuples.get(i).get(j));

        Mat labels = new Mat();
        Mat centers = new Mat();
        double variance = Core.kmeans(data, numClusters, labels, new TermCriteria(TermCriteria.COUNT, DEFAULT_KMEANS_MAX_ITER, 0), 1,
            Core.KMEANS_PP_CENTERS, centers);

        Map<Tuple, Integer> clusterLabels = new HashMap<>();
        for (int i = 0; i < tuples.size(); i++)
            clusterLabels.put(tuples.get(i), (int) labels.get(i, 0)[0]);

        List<Tuple> clusterCenters = new ArrayList<>();
        for (int i = 0; i < numClusters; i++) {
            Tuple center = new Tuple();
            for (int j = 0; j < numDimensions; j++)
                center.add(centers.get(i, j)[0]);
            clusterCenters.add(center);
        }

        return new Clusters(variance, clusterLabels, clusterCenters);
    }

    public Map<BufferedImage, String> batchFindText(Collection<BufferedImage> images, OcrOptions options) {
        try (TessBaseAPI api = new TessBaseAPI()) {
            if (api.Init("./data", "eng") != 0)
                throw new IllegalStateException("Could not initialize tesseract.");
            if (options.allowedCharacters != null)
                api.SetVariable("tessedit_char_whitelist", options.allowedCharacters);
            if (options.singleCharacter) {
                api.SetPageSegMode(10);
                api.SetVariable("load_system_dawg", "0");
                api.SetVariable("load_freq_dawg", "0");
            }
            Map<BufferedImage, String> result = new HashMap<>();
            images.forEach(image -> {
                MatOfByte bytes = new MatOfByte();
                Imgcodecs.imencode(".tif", toMat(image), bytes);
                ByteBuffer buffer = ByteBuffer.wrap(bytes.toArray());
                try (PIX pix = lept.pixReadMem(buffer, buffer.capacity())) {
                    api.SetImage(pix);
                    api.SetRectangle(
                        (int) (image.getWidth() * (1 - options.fullness) / 2),
                        (int) (image.getHeight() * (1 - options.fullness) / 2),
                        (int) (image.getWidth() * options.fullness),
                        (int) (image.getHeight() * options.fullness));
                    try (BytePointer textPtr = api.GetUTF8Text();
                            IntPointer confidencePtr = api.AllWordConfidences()) {
                        result.put(image, confidencePtr.get(0) >= options.confidenceThreshold ? textPtr.getString() : "");
                    }
                }
            });
            return result;
        }
    }

    private Mat toMat(BufferedImage image) {
        BufferedImage newImage = ImageUtils.copy(image);
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

    public static class Tuple extends ArrayList<Double> {

        private static final long serialVersionUID = 1L;

        public Tuple(double... values) {
            for (double value : values)
                add(value);
        }
    }

    @Data
    public static class Clusters {

        private final double variance;
        private final Map<Tuple, Integer> labels;
        private final List<Tuple> centers;
    }

    @Data
    public static class OcrOptions {

        private String allowedCharacters;
        private boolean singleCharacter = false;

        /**
         * A number from 0 to 1 representing how much of the image should be searched for
         * characters. For example, 0.8 represents a sub-image with the same center but with a width
         * and height 4/5 of the original. This setting is useful to avoid borders, which can be
         * very detrimental to Tesseract OCR results.
         */
        private double fullness = 0.8;

        /**
         * A number from 0 to 100 representing how confident the OCR result needs to be in order to
         * accept it. The higher this value, the more confidence is required.
         */
        private int confidenceThreshold = 50;
    }
}
