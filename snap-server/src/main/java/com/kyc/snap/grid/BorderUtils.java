package com.kyc.snap.grid;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.kyc.snap.image.ImageUtils;

public class BorderUtils {

    public static Border computeRightBorder(BufferedImage image, int startX, int startY, int width, int height) {
        int medianRgb = ImageUtils.medianRgb(image, startX, startY, width, height);
        List<Border> borders = new ArrayList<>();
        for (int i = 0; i < height; i++)
            borders.add(findRightBorder(image, startY + i, startX, width, medianRgb));
        Collections.sort(borders, Comparator.comparingInt(Border::getWidth));
        return borders.get(borders.size() / 2);
    }

    public static Border computeBottomBorder(BufferedImage image, int startX, int startY, int width, int height) {
        int medianRgb = ImageUtils.medianRgb(image, startX, startY, width, height);
        List<Border> borders = new ArrayList<>();
        for (int j = 0; j < width; j++)
            borders.add(findBottomBorder(image, startX + j, startY, height, medianRgb));
        Collections.sort(borders, Comparator.comparingInt(Border::getWidth));
        return borders.get(borders.size() / 2);
    }

    private static Border findRightBorder(BufferedImage image, int y, int startX, int width, int medianRgb) {
        Border thickestBorder = new Border(-1, 0);
        int start = -1;
        for (int j = 0; j < width; j++)
            if (ImageUtils.inBounds(image, startX + j, y) && !ImageUtils.isDifferent(image.getRGB(startX + j, y), medianRgb)) {
                int thickness = j - start - 1;
                if (thickness > thickestBorder.getWidth())
                    thickestBorder = new Border(ImageUtils.medianRgb(image, startX + start, y, thickness, 1), thickness);
                start = j;
            }
        return thickestBorder;
    }

    private static Border findBottomBorder(BufferedImage image, int x, int startY, int height, int medianRgb) {
        Border thickestBorder = new Border(-1, 0);
        int start = -1;
        for (int i = 0; i < height; i++)
            if (ImageUtils.inBounds(image, x, startY + i) && !ImageUtils.isDifferent(image.getRGB(x, startY + i), medianRgb)) {
                int thickness = i - start - 1;
                if (thickness > thickestBorder.getWidth())
                    thickestBorder = new Border(ImageUtils.medianRgb(image, x, startY + start, thickness, 1), thickness);
                start = i;
            }
        return thickestBorder;
    }
}
