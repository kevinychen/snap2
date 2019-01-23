package com.kyc.snap.image;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

import javax.imageio.ImageIO;

import com.kyc.snap.grid.Border;

import lombok.Data;

public class ImageUtils {

    public static BufferedImage copy(BufferedImage image) {
        BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        for (int x = 0; x < image.getWidth(); x++)
            for (int y = 0; y < image.getHeight(); y++)
                newImage.setRGB(x, y, image.getRGB(x, y));
        return newImage;
    }

    public static BufferedImage rotate90DegreesClockwise(BufferedImage image) {
        BufferedImage newImage = new BufferedImage(image.getHeight(), image.getWidth(), BufferedImage.TYPE_3BYTE_BGR);
        for (int x = 0; x < image.getWidth(); x++)
            for (int y = 0; y < image.getHeight(); y++)
                newImage.setRGB(image.getHeight() - y - 1, x, image.getRGB(x, y));
        return newImage;
    }

    public static BufferedImage scale(BufferedImage image, double scale) {
        int newWidth = (int) (image.getWidth() * scale);
        int newHeight = (int) (image.getHeight() * scale);
        BufferedImage newImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = newImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, newWidth, newHeight, null);
        g.dispose();
        return newImage;
    }

    public static BufferedImage fromBytes(byte[] bytes) {
        try {
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] toBytes(BufferedImage image) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toDataURL(BufferedImage image) {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(toBytes(image));
    }

    public static int medianRgb(BufferedImage image, int startX, int startY, int width, int height) {
        List<Integer> reds = new ArrayList<>();
        List<Integer> greens = new ArrayList<>();
        List<Integer> blues = new ArrayList<>();
        for (int x = startX; x < startX + width; x++)
            for (int y = startY; y < startY + height; y++) {
                int rgb = image.getRGB(x, y);
                reds.add((rgb >> 16) & 0xff);
                greens.add((rgb >> 8) & 0xff);
                blues.add((rgb >> 0) & 0xff);
            }
        Collections.sort(reds);
        Collections.sort(greens);
        Collections.sort(blues);
        int medianRed = reds.get(reds.size() / 2);
        int medianGreen = greens.get(reds.size() / 2);
        int medianBlue = blues.get(reds.size() / 2);
        return (medianRed << 16) | (medianGreen << 8) | (medianBlue << 0);
    }

    public static Border findVerticalBorder(BufferedImage image) {
        int leftRgb = medianRgb(image, 0, 0, image.getWidth() / 3, image.getHeight());
        int rightRgb = medianRgb(image, image.getWidth() * 2 / 3, 0, image.getWidth() / 3, image.getHeight());

        List<Integer> medianRgbs = new ArrayList<>();
        for (int x = 0; x < image.getWidth(); x++)
            medianRgbs.add(medianRgb(image, x, 0, 1, image.getHeight()));

        Border border = Border.NONE;
        int minCenterProximity = Integer.MAX_VALUE;
        int start = 0;
        while (start < medianRgbs.size()) {
            int end = start;
            int sumRed = 0, sumGreen = 0, sumBlue = 0;
            while (end < medianRgbs.size() && isDifferent(medianRgbs.get(end), leftRgb) & isDifferent(medianRgbs.get(end), rightRgb)) {
                sumRed += (medianRgbs.get(end) >> 16) & 0xff;
                sumGreen += (medianRgbs.get(end) >> 8) & 0xff;
                sumBlue += (medianRgbs.get(end) >> 0) & 0xff;
                end++;
            }
            int centerProximity = Math.abs(start + end - image.getWidth());
            if (end > start && centerProximity < minCenterProximity) {
                int borderWidth = end - start;
                int averageRgb = ((sumRed / borderWidth) << 16) | ((sumGreen / borderWidth) << 8) | ((sumBlue / borderWidth) << 0);
                border = new Border(averageRgb, borderWidth);
                minCenterProximity = centerProximity;
            }
            start = end + 1;
        }
        return border;
    }

    public static List<Blob> findBlobs(BufferedImage image, Predicate<Integer> isBlob) {
        boolean[][] visited = new boolean[image.getHeight()][image.getWidth()];
        List<Blob> blobs = new ArrayList<>();
        for (int x = 0; x < image.getWidth(); x++)
            for (int y = 0; y < image.getHeight(); y++)
                if (!visited[y][x] && isBlob.test(image.getRGB(x, y))) {
                    int minX = Integer.MAX_VALUE;
                    int minY = Integer.MAX_VALUE;
                    int maxX = Integer.MIN_VALUE;
                    int maxY = Integer.MIN_VALUE;
                    Deque<Point> floodfill = new ArrayDeque<>();
                    floodfill.add(new Point(x, y));
                    List<Point> points = new ArrayList<>();
                    while (!floodfill.isEmpty()) {
                        Point p = floodfill.removeFirst();
                        if (p.x >= 0 && p.x < image.getWidth() && p.y >= 0 && p.y < image.getHeight() && !visited[p.y][p.x]
                                && isBlob.test(image.getRGB(p.x, p.y))) {
                            visited[p.y][p.x] = true;
                            if (p.x < minX)
                                minX = p.x;
                            if (p.y < minY)
                                minY = p.y;
                            if (p.x > maxX)
                                maxX = p.x;
                            if (p.y > maxY)
                                maxY = p.y;
                            points.add(p);
                            floodfill.addLast(new Point(p.x - 1, p.y));
                            floodfill.addLast(new Point(p.x + 1, p.y));
                            floodfill.addLast(new Point(p.x, p.y - 1));
                            floodfill.addLast(new Point(p.x, p.y + 1));
                        }
                    }
                    blobs.add(new Blob(minX, minY, maxX - minX + 1, maxY - minY + 1, points));
                }
        return blobs;
    }

    public static BufferedImage getBlobImage(BufferedImage image, Blob blob) {
        BufferedImage blobImage = new BufferedImage(blob.getWidth(), blob.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        for (Point p : blob.getPoints())
            blobImage.setRGB(p.x - blob.getX(), p.y - blob.getY(), image.getRGB(p.x, p.y));
        return blobImage;
    }

    public static BufferedImage makeTransparent(BufferedImage image, double opacity) {
        BufferedImage transparentImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        int alphaBits = (int) (opacity * 255);
        for (int x = 0; x < image.getWidth(); x++)
            for (int y = 0; y < image.getHeight(); y++)
                transparentImage.setRGB(x, y, (alphaBits << 24) + (image.getRGB(x, y) & 0xffffff));
        return transparentImage;
    }

    public static boolean isLight(int rgb) {
        return ((rgb >> 16) & 0xff) + ((rgb >> 8) & 0xff) + ((rgb >> 0) & 0xff) > 3 * 128;
    }

    public static boolean isDifferent(int rgb1, int rgb2) {
        int redDiff = ((rgb1 >> 16) & 0xff) - ((rgb2 >> 16) & 0xff);
        int greenDiff = ((rgb1 >> 8) & 0xff) - ((rgb2 >> 8) & 0xff);
        int blueDiff = ((rgb1 >> 0) & 0xff) - ((rgb2 >> 0) & 0xff);
        return redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff > 3 * 64 * 64;
    }

    @Data
    public static class Blob {

        private final int x;
        private final int y;
        private final int width;
        private final int height;
        private final List<Point> points;
    }

    private ImageUtils() {}
}
