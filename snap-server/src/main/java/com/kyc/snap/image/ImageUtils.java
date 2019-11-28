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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import com.google.common.collect.ImmutableList;
import com.kyc.snap.grid.Border;

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
        BufferedImage newImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_4BYTE_ABGR);
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

    public static byte[] toBytesCompressed(BufferedImage image) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageOutputStream imageOut = new MemoryCacheImageOutputStream(out);
        ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
        jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        jpgWriteParam.setCompressionQuality(0.7f);
        jpgWriter.setOutput(imageOut);
        try {
            jpgWriter.write(null, new IIOImage(image, null, null), jpgWriteParam);
            return out.toByteArray();
        } catch (IOException e) {
            return toBytes(image); // fallback
        } finally {
            jpgWriter.dispose();
        }
    }

    public static String toDataURL(BufferedImage image) {
        return "data:image/jpg;base64," + Base64.getEncoder().encodeToString(toBytesCompressed(image));
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

    public static List<ImageBlob> findBlobs(BufferedImage image, boolean exact) {
        BiFunction<Integer, Integer, Boolean> similar = (rgb1, rgb2) -> exact ? rgb1 == rgb2 : !isDifferent(rgb1, rgb2);
        boolean[][] visited = new boolean[image.getHeight()][image.getWidth()];
        for (int x = 0; x < image.getWidth(); x++)
            for (int y = 0; y < image.getHeight(); y++)
                if (isBorder(image, x, y) && !visited[y][x]) {
                    Deque<Point> floodfill = new ArrayDeque<>();
                    floodfill.add(new Point(x, y));
                    while (!floodfill.isEmpty()) {
                        Point p = floodfill.removeFirst();
                        if (inBounds(image, p.x, p.y) && !visited[p.y][p.x] && similar.apply(image.getRGB(x, y), image.getRGB(p.x, p.y))) {
                            visited[p.y][p.x] = true;
                            for (Point borderingPoint : borderingPoints(p))
                                floodfill.addLast(borderingPoint);
                        }
                    }
                }

        List<ImageBlob> blobs = new ArrayList<>();
        for (int x = 0; x < image.getWidth(); x++)
            for (int y = 0; y < image.getHeight(); y++)
                if (!visited[y][x]) {
                    int minX = Integer.MAX_VALUE;
                    int minY = Integer.MAX_VALUE;
                    int maxX = Integer.MIN_VALUE;
                    int maxY = Integer.MIN_VALUE;
                    Deque<Point> floodfill = new ArrayDeque<>();
                    floodfill.add(new Point(x, y));
                    Set<Point> innerPoints = new HashSet<>();
                    while (!floodfill.isEmpty()) {
                        Point p = floodfill.removeFirst();
                        if (inBounds(image, p.x, p.y) && !visited[p.y][p.x]) {
                            visited[p.y][p.x] = true;
                            if (p.x < minX)
                                minX = p.x;
                            if (p.y < minY)
                                minY = p.y;
                            if (p.x > maxX)
                                maxX = p.x;
                            if (p.y > maxY)
                                maxY = p.y;
                            innerPoints.add(p);
                            for (Point borderingPoint : borderingPoints(p))
                                floodfill.addLast(borderingPoint);
                        }
                    }
                    Set<Point> fencePoints = new HashSet<>();
                    for (Point innerPoint : innerPoints)
                        for (Point borderingPoint : borderingPoints(innerPoint))
                            if (!innerPoints.contains(borderingPoint))
                                fencePoints.add(borderingPoint);
                    blobs.add(new ImageBlob(minX, minY, maxX - minX + 1, maxY - minY + 1, fencePoints, innerPoints.iterator().next()));
                }
        return blobs;
    }

    public static BufferedImage getBlobImage(BufferedImage image, ImageBlob blob) {
        BufferedImage blobImage = new BufferedImage(blob.getWidth(), blob.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        boolean[][] visited = new boolean[image.getHeight()][image.getWidth()];
        Deque<Point> floodfill = new ArrayDeque<>();
        floodfill.add(blob.getInnerPoint());
        while (!floodfill.isEmpty()) {
            Point p = floodfill.removeFirst();
            if (inBounds(image, p.x, p.y) && !visited[p.y][p.x] && !blob.getFencePoints().contains(p)) {
                visited[p.y][p.x] = true;
                blobImage.setRGB(p.x - blob.getX(), p.y - blob.getY(), image.getRGB(p.x, p.y));
                for (Point borderingPoint : borderingPoints(p))
                    floodfill.addLast(borderingPoint);
            }
        }
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

    public static boolean inBounds(BufferedImage image, int x, int y) {
        return x >= 0 && x < image.getWidth() && y >= 0 && y < image.getHeight();
    }

    public static boolean isBorder(BufferedImage image, int x, int y) {
        return x == 0 || x == image.getWidth() - 1 || y == 0 || y == image.getHeight() - 1;
    }

    public static List<Point> borderingPoints(Point p) {
        return ImmutableList.of(new Point(p.x - 1, p.y), new Point(p.x + 1, p.y), new Point(p.x, p.y - 1), new Point(p.x, p.y + 1));
    }

    private ImageUtils() {}
}
