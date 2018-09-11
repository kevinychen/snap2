package com.kyc.snap.grid;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.kyc.snap.google.GoogleAPIManager;
import com.kyc.snap.grid.Grid.Square;
import com.kyc.snap.grid.GridPosition.Col;
import com.kyc.snap.grid.GridPosition.Row;
import com.kyc.snap.image.ImageUtils;
import com.kyc.snap.opencv.OpenCvManager;
import com.kyc.snap.opencv.OpenCvManager.Line;

import lombok.Data;

@Data
public class GridParser {

    private final OpenCvManager openCv;
    private final GoogleAPIManager googleApi;

    public GridLines findGridLines(BufferedImage image, int approxGridSquareSize) {
        List<Line> cardinalLines = openCv.findLines(image, Math.PI / 2, approxGridSquareSize / 2);

        TreeSet<Integer> horizontalLines = new TreeSet<>();
        TreeSet<Integer> verticalLines = new TreeSet<>();
        for (Line line : cardinalLines) {
            boolean horizontal = line.getY1() == line.getY2();
            boolean vertical = line.getX1() == line.getX2();
            Preconditions.checkArgument(horizontal != vertical,
                "Expected horizontal or vertical line but got %s", line);
            if (horizontal)
                horizontalLines.add((int) line.getY1());
            else
                verticalLines.add((int) line.getX1());
        }

        return new GridLines(
            deduplicateCloseMarks(horizontalLines, approxGridSquareSize / 4),
            deduplicateCloseMarks(verticalLines, approxGridSquareSize / 4));
    }

    public GridPosition getGridPosition(GridLines lines) {
        List<Integer> horizontalLines = new ArrayList<>(lines.getHorizontalLines());
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i + 1 < horizontalLines.size(); i++)
            rows.add(new Row(horizontalLines.get(i), horizontalLines.get(i + 1) - horizontalLines.get(i)));

        List<Integer> verticalLines = new ArrayList<>(lines.getVerticalLines());
        List<Col> cols = new ArrayList<>();
        for (int i = 0; i + 1 < verticalLines.size(); i++)
            cols.add(new Col(verticalLines.get(i), verticalLines.get(i + 1) - verticalLines.get(i)));

        return new GridPosition(rows, cols);
    }

    /**
     * Return a grid position that has identically spaced rows and identically spaced heights.
     */
    public GridPosition getEvenGridPosition(GridLines lines, int approxGridSquareSize) {
        EvenSequence horizontalSequence = findEvenSequence(lines.getHorizontalLines(), approxGridSquareSize);
        List<Row> rows = new ArrayList<>();
        for (double line = horizontalSequence.start; line < horizontalSequence.end; line += horizontalSequence.period)
            rows.add(new Row((int) line, (int) horizontalSequence.period));

        EvenSequence verticalSequence = findEvenSequence(lines.getVerticalLines(), approxGridSquareSize);
        List<Col> cols = new ArrayList<>();
        for (double line = verticalSequence.start; line < verticalSequence.end; line += verticalSequence.period)
            cols.add(new Col((int) line, (int) verticalSequence.period));

        return new GridPosition(rows, cols);
    }

    public Grid parseGrid(BufferedImage image, GridPosition pos, boolean parseText) {
        Map<GridLocation, String> allText = new HashMap<>();
        if (parseText) {
            Map<GridLocation, BufferedImage> subimages = new HashMap<>();
            for (Row row : pos.getRows())
                for (Col col : pos.getCols()) {
                    subimages.put(
                        new GridLocation(row, col),
                        image.getSubimage(col.getStartX(), row.getStartY(), col.getWidth(), row.getHeight()));
                }
            Map<BufferedImage, String> text = googleApi.batchFindText(subimages.values());
            allText.putAll(Maps.transformValues(subimages, text::get));
        }

        Square[][] squares = new Square[pos.getNumRows()][pos.getNumCols()];
        for (int i = 0; i < pos.getNumRows(); i++)
            for (int j = 0; j < pos.getNumCols(); j++) {
                Row row = pos.getRows().get(i);
                Col col = pos.getCols().get(j);
                int medianRgb = ImageUtils.medianRgb(image, col.getStartX(), row.getStartY(), col.getWidth(), row.getHeight());
                String text = allText.getOrDefault(new GridLocation(row, col), "");
                squares[i][j] = new Square(medianRgb, text);
            }
        return new Grid(squares);
    }

    private static TreeSet<Integer> deduplicateCloseMarks(TreeSet<Integer> marks, int minError) {
        List<Integer> orderedMarks = new ArrayList<>(marks);
        TreeSet<Integer> deduplicatedMarks = new TreeSet<>();
        int start = 0;
        while (start < orderedMarks.size()) {
            int end = start;
            int sumMarks = 0;
            while (end < orderedMarks.size() && orderedMarks.get(end) - orderedMarks.get(start) < minError) {
                sumMarks += orderedMarks.get(end);
                end++;
            }
            deduplicatedMarks.add(sumMarks / (end - start));
            start = end;
        }
        return deduplicatedMarks;
    }

    private static EvenSequence findEvenSequence(TreeSet<Integer> marks, int approxPeriod) {
        int highestMark = marks.last();
        int minError = approxPeriod / 4;

        int betterApproxPeriod = -1;
        long bestPeriodScore = Long.MAX_VALUE;
        for (int period = approxPeriod / 2; period < highestMark / 2; period++) {
            long score = 0;
            for (int mark : marks) {
                int shiftedMark = mark + period;
                int closestDistance = closestDistance(marks, shiftedMark);
                score += Math.pow(Math.min(closestDistance, minError), 2);
            }
            if (score < bestPeriodScore) {
                betterApproxPeriod = period;
                bestPeriodScore = score;
            }
        }

        double bestPeriod = Double.NaN;
        int bestOffset = -1;
        long bestScore = Long.MAX_VALUE;
        for (double period = betterApproxPeriod - 2; period <= betterApproxPeriod + 2; period += 1. / 16)
            for (int offset = 0; offset < period; offset++) {
                TreeSet<Integer> offsetMarks = new TreeSet<>();
                for (double mark = offset - period; mark < highestMark + period; mark += period)
                    offsetMarks.add((int) mark);
                long score = 0;
                for (int mark : marks) {
                    int closestDistance = closestDistance(offsetMarks, mark);
                    score += Math.pow(Math.min(closestDistance, minError), 2);
                }
                if (score < bestScore) {
                    bestPeriod = period;
                    bestOffset = offset;
                    bestScore = score;
                }
            }

        List<Integer> goodOffsets = new ArrayList<>();
        for (double offset = bestOffset; offset < highestMark + bestPeriod; offset += bestPeriod)
            if (closestDistance(marks, (int) offset) <= minError)
                goodOffsets.add((int) offset);

        return new EvenSequence(bestPeriod, goodOffsets.get(0), goodOffsets.get(goodOffsets.size() - 1));
    }

    private static int closestDistance(TreeSet<Integer> nums, int target) {
        int closestDistance = Integer.MAX_VALUE;
        Integer higher = nums.higher(target);
        if (higher != null && higher - target < closestDistance)
            closestDistance = higher - target;
        Integer lower = nums.lower(target);
        if (lower != null && target - lower < closestDistance)
            closestDistance = target - lower;
        return closestDistance;
    }

    @Data
    private static class EvenSequence {

        private final double period;
        private final int start;
        private final int end;
    }

    @Data
    private static class GridLocation {

        private final Row row;
        private final Col col;
    }
}
