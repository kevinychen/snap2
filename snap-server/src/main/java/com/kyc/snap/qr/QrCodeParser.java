package com.kyc.snap.qr;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.junit.Test;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.FormatString;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * A QR code decoder according to https://www.thonky.com/qr-code-tutorial/introduction.
 */
public class QrCodeParser {

    /**
     * The 8 possible masking functions, specified by the 3rd, 4th, and 5th bits of the
     * {@link FormatString}.
     */
    private static final List<BiFunction<Integer, Integer, Boolean>> MASK_FUNCTIONS = ImmutableList.of(
        (row, col) -> (row + col) % 2 == 0,
        (row, col) -> row % 2 == 0,
        (row, col) -> col % 3 == 0,
        (row, col) -> (row + col) % 3 == 0,
        (row, col) -> (row / 2 + col / 3) % 2 == 0,
        (row, col) -> (row * col % 2 + row * col % 3) == 0,
        (row, col) -> (row * col % 2 + row * col % 3) % 2 == 0,
        (row, col) -> ((row + col) % 2 + (row * col % 3)) % 2 == 0);

    /**
     * Decodes a QR code where each grid square ("module") denotes the probability that the module
     * is black.
     */
    public String decode(double[][] grid) {
        Preconditions.checkArgument(grid.length >= 21, "Grid should have length at least 21");
        Preconditions.checkArgument(grid.length % 4 == 1, "Grid should have length one more than a multiple of 4");
        for (double[] row : grid) {
            Preconditions.checkArgument(row.length == grid.length, "Grid is not square");
            for (double prob : row)
                Preconditions.checkArgument(prob >= 0 && prob <= 1, "Grid probability not between 0 and 1 inclusive");
        }

        FormatStringPoints formatStringPoints = getFormatStringPoints(grid.length);
        double[] formatProbabilities = new double[32];
        for (int formatString = 0; formatString < 32; formatString++) {
            formatProbabilities[formatString] = 1;
            int fullFormatString = toFullFormatString(formatString);
            for (int i = 0; i < 15; i++) {
                int fullFormatStringBit = (fullFormatString >> (14 - i)) % 2;
                Point p1 = formatStringPoints.points1.get(i);
                Point p2 = formatStringPoints.points2.get(i);
                double prob1 = grid[p1.y][p1.x];
                double prob2 = grid[p2.y][p2.x];
                formatProbabilities[formatString] *= fullFormatStringBit == 1 ? prob1 * prob2 : (1 - prob1) * (1 - prob2);
            }
        }
//        for (int formatString = 0; formatString < 32; formatString++)
//            System.out.println(formatString + " " + formatProbabilities[formatString]);

        // TODO
        return null;
    }

    private static FormatStringPoints getFormatStringPoints(int size) {
        List<Point> points1 = new ArrayList<>();
        for (int x = 0; x <= 8; x++)
            if (x != 6)
                points1.add(new Point(x, 8));
        for (int y = 7; y >= 0; y--)
            points1.add(new Point(8, y));

        List<Point> points2 = new ArrayList<>();
        for (int y = size - 1; y > size - 8; y--)
            points2.add(new Point(8, y));
        for (int x = size - 8; x < size; x++)
            points2.add(new Point(x, 8));

        return new FormatStringPoints(points1, points2);
    }

    /**
     * The location of the 15 bits that store format information. This location is duplicated in two
     * separate locations for redundancy.
     */
    @Data
    private static class FormatStringPoints {

        final List<Point> points1;
        final List<Point> points2;
    }

    /**
     * Given a 5-bit format string, return the 15-bit format string with error correction.
     */
    private static int toFullFormatString(int formatString) {
        formatString *= 1024;
        int errorCorrection = formatString;
        while (errorCorrection >= 1024) {
            int subtract = 0x537;
            while (subtract * 2 < errorCorrection)
                subtract *= 2;
            errorCorrection ^= subtract;
        }
        return formatString + errorCorrection ^ 0x5412;
    }

    /**
     * Returns the modules where data bits or error correction bits are located, in order of
     * placement. See
     * https://en.wikipedia.org/wiki/QR_code#/media/File:QR_Ver3_Codeword_Ordering.svg.
     */
    private static List<Point> getDataPoints(int size) {
        boolean[][] reserved = new boolean[size][size];

        // finder patterns and separators
        for (int x = 0; x < 8; x++)
            for (int y = 0; y < 8; y++)
                reserved[y][x] = reserved[y][x + size - 8] = reserved[y + size - 8][x] = true;

        // alignment patterns
        List<Integer> alignmentPatternLocations = getAlignmentPatternLocations(size);
        for (int x : alignmentPatternLocations)
            for (int y : alignmentPatternLocations)
                if ((x >= 10 || y >= 10) && (x < size - 10 || y >= 10) && (x >= 10 || y < size - 10))
                    for (int dx = -2; dx <= 2; dx++)
                        for (int dy = -2; dy <= 2; dy++)
                            reserved[y + dy][x + dx] = true;

        // timing patterns
        for (int i = 0; i < size; i++)
            reserved[i][6] = reserved[6][i] = true;

        // dark module
        reserved[size - 8][8] = true;

        // format string modules
        FormatStringPoints formatStringPoints = getFormatStringPoints(size);
        for (Point p : formatStringPoints.points1)
            reserved[p.y][p.x] = true;
        for (Point p : formatStringPoints.points2)
            reserved[p.y][p.x] = true;

        List<Point> dataPoints = new ArrayList<>();
        boolean upwards = true;
        for (int x = size - 1; x >= 0; x -= 2) {
            // skip vertical timing pattern
            if (x == 6)
                x--;
            if (upwards) {
                for (int y = size - 1; y >= 0; y--)
                    for (int dx = 0; dx >= -1; dx--)
                        if (!reserved[y][x + dx])
                            dataPoints.add(new Point(x + dx, y));
            } else {
               for (int y = 0; y < size; y++)
                    for (int dx = 0; dx >= -1; dx--)
                        if (!reserved[y][x + dx])
                            dataPoints.add(new Point(x + dx, y));
            }
            upwards = !upwards;
        }

        return dataPoints;
    }

    /**
     * https://stackoverflow.com/questions/13238704/calculating-the-position-of-qr-code-alignment-
     * patterns
     */
    private static List<Integer> getAlignmentPatternLocations(int size) {
        int version = size / 4 - 4;
        if (version == 1)
            return ImmutableList.of();
        int divs = 2 + version / 7;
        int step = (size + divs - 13) / (2 * (divs - 1)) * 2;
        List<Integer> locations = new ArrayList<>();
        locations.add(6);
        for (int i = divs - 2; i >= 0; i--)
            locations.add(size - 7 - i * step);
        return locations;
    }

    @AllArgsConstructor
    private static enum ErrorCorrectionLevel {

        L(1),
        M(0),
        Q(3),
        H(2);

        /**
         * The first 2-bits of the {@link FormatString}.
         */
        final int format;
    }

    private final double[][] HELLO_WORLD_GRID = {
            { 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1 },
            { 1, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1 },
            { 1, 0, 1, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1 },
            { 1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 1 },
            { 1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1, 1, 1, 0, 1 },
            { 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1 },
            { 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 1, 1, 1, 1, 1, 1 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 1, 0, 1, 1, 1, 1, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 1, 0, 1, 0 },
            { 1, 0, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 1, 1, 1, 0 },
            { 0, 0, 1, 0, 1, 0, 1, 1, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0 },
            { 1, 0, 1, 1, 0, 1, 0, 0, 0, 1, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0 },
            { 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1 },
            { 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0 },
            { 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 1, 1 },
            { 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 1, 1 },
            { 1, 0, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 0, 1, 0, 0, 0, 1, 1, 1 },
            { 1, 0, 1, 1, 1, 0, 1, 0, 1, 0, 1, 1, 1, 0, 0, 0, 1, 0, 1, 0, 0 },
            { 1, 0, 1, 1, 1, 0, 1, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1 },
            { 1, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1, 0, 0, 1, 1, 1, 0, 0, 1, 1, 0 },
            { 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0 }
    };

    @Test
    public void test() {
        List<Point> result = getDataPoints(25);
        for (Point p : result)
        System.out.println(p);
//        QrCodeParser q = new QrCodeParser();
//        q.decode(HELLO_WORLD_GRID);
    }
}
