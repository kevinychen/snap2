package com.kyc.snap.image;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

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

    public static boolean isLight(int rgb) {
        return ((rgb >> 16) & 0xff) + ((rgb >> 8) & 0xff) + ((rgb >> 0) & 0xff) > 3 * 128;
    }

    public static boolean isDifferent(int rgb1, int rgb2) {
        int redDiff = ((rgb1 >> 16) & 0xff) - ((rgb2 >> 16) & 0xff);
        int greenDiff = ((rgb1 >> 8) & 0xff) - ((rgb2 >> 8) & 0xff);
        int blueDiff = ((rgb1 >> 0) & 0xff) - ((rgb2 >> 0) & 0xff);
        return redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff > 3 * 64 * 64;
    }

    private ImageUtils() {}
}
