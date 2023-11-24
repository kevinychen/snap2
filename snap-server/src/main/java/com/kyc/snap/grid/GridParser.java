package com.kyc.snap.grid;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.function.BiFunction;

import com.kyc.snap.document.Document.DocumentText;
import com.kyc.snap.document.Rectangle;
import com.kyc.snap.grid.Border.Style;
import com.kyc.snap.grid.Grid.Square;
import com.kyc.snap.grid.GridPosition.Col;
import com.kyc.snap.grid.GridPosition.Row;
import com.kyc.snap.image.ImageBlob;
import com.kyc.snap.image.ImageUtils;
import com.kyc.snap.opencv.OpenCvManager;
import com.kyc.snap.opencv.OpenCvManager.Clusters;
import com.kyc.snap.opencv.OpenCvManager.Line;
import com.kyc.snap.opencv.OpenCvManager.Tuple;
import com.kyc.snap.util.Utils;

public record GridParser(OpenCvManager openCv) {

    /**
     * Minimum difference between widths of two borders in order for them to have different styles.
     */
    public static final double MIN_WIDTH_DIFF_BETWEEN_BORDER_STYLES = 1.5;

    public GridLines findGridLines(BufferedImage image) {
        List<Line> cardinalLines = openCv.findLines(
                image,
                Math.PI / 2,
                (image.getWidth() + image.getHeight()) / 100.);

        if (cardinalLines.size() >= 10000)
            return new GridLines(
                    new TreeSet<>(List.of(0, image.getHeight())),
                    new TreeSet<>(List.of(0, image.getWidth())));

        List<List<Line>> connectedComponents = Utils.findConnectedComponents(cardinalLines,
                (line1, line2) -> Math.max(line1.x1(), line1.x2()) + 32 > Math.min(line2.x1(), line2.x2())
                        && Math.max(line2.x1(), line2.x2()) + 32 > Math.min(line1.x1(), line1.x2())
                        && Math.max(line1.y1(), line1.y2()) + 32 > Math.min(line2.y1(), line2.y2())
                        && Math.max(line2.y1(), line2.y2()) + 32 > Math.min(line1.y1(), line1.y2()));

        TreeSet<Integer> horizontalLines = new TreeSet<>();
        TreeSet<Integer> verticalLines = new TreeSet<>();
        for (List<Line> component : connectedComponents)
            if (component.size() >= 8) {
                for (Line line : component)
                    if (line.y1() == line.y2())
                        horizontalLines.add((int) line.y1());
                    else if (line.x1() == line.x2())
                        verticalLines.add((int) line.x1());
                    else
                        throw new IllegalStateException("Expected horizontal or vertical line");
            }

        if (horizontalLines.isEmpty() || verticalLines.isEmpty())
            return new GridLines(
                    new TreeSet<>(List.of(0, image.getHeight())),
                    new TreeSet<>(List.of(0, image.getWidth())));

        double horizontalPeriod = Utils.findApproxPeriod(horizontalLines);
        double verticalPeriod = Utils.findApproxPeriod(verticalLines);
        return new GridLines(
                Utils.deduplicateCloseMarks(horizontalLines, (int) horizontalPeriod / 4),
                Utils.deduplicateCloseMarks(verticalLines, (int) verticalPeriod / 4));
    }

    public GridLines getInterpolatedGridLines(GridLines lines) {
        return new GridLines(
                Utils.findInterpolatedSequence(lines.horizontalLines()),
                Utils.findInterpolatedSequence(lines.verticalLines()));
    }

    public GridLines findImplicitGridLines(BufferedImage image) {
        List<ImageBlob> blobs = ImageUtils.findBlobs(image, false);
        TreeSet<Integer> rows = Utils.findInterpolatedSequence(blobs.stream()
                .map(blob -> blob.y() + blob.height() / 2)
                .toList());
        TreeSet<Integer> cols = Utils.findInterpolatedSequence(blobs.stream()
                .map(blob -> blob.x() + blob.width() / 2)
                .toList());

        BiFunction<TreeSet<Integer>, Integer, TreeSet<Integer>> findSurroundingMarksFunction = (marks, limit) -> {
            if (marks.size() < 2)
                return new TreeSet<>(List.of(0, limit));
            List<Integer> sortedMarks = new ArrayList<>(marks);
            TreeSet<Integer> surroundingMarks = new TreeSet<>();
            surroundingMarks.add(Math.max(0, sortedMarks.get(0) - (sortedMarks.get(1) - sortedMarks.get(0)) / 2));
            for (int i = 1; i < sortedMarks.size(); i++)
                surroundingMarks.add((sortedMarks.get(i - 1) + sortedMarks.get(i)) / 2);
            surroundingMarks.add(Math.min(limit, sortedMarks.get(sortedMarks.size() - 1)
                    + (sortedMarks.get(sortedMarks.size() - 1) - sortedMarks.get(sortedMarks.size() - 2)) / 2));
            return surroundingMarks;
        };

        return new GridLines(
                findSurroundingMarksFunction.apply(rows, image.getHeight()),
                findSurroundingMarksFunction.apply(cols, image.getWidth()));
    }

    public GridPosition getGridPosition(GridLines lines) {
        List<Integer> horizontalLines = new ArrayList<>(lines.horizontalLines());
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i + 1 < horizontalLines.size(); i++)
            rows.add(new Row(horizontalLines.get(i), horizontalLines.get(i + 1) - horizontalLines.get(i)));

        List<Integer> verticalLines = new ArrayList<>(lines.verticalLines());
        List<Col> cols = new ArrayList<>();
        for (int i = 0; i + 1 < verticalLines.size(); i++)
            cols.add(new Col(verticalLines.get(i), verticalLines.get(i + 1) - verticalLines.get(i)));

        return new GridPosition(rows, cols);
    }

    public void findGridColors(BufferedImage image, GridPosition pos, Grid grid) {
        for (int i = 0; i < pos.getNumRows(); i++)
            for (int j = 0; j < pos.getNumCols(); j++) {
                Row row = pos.rows().get(i);
                Col col = pos.cols().get(j);
                int medianRgb = ImageUtils.medianRgb(image, col.startX(), row.startY(), col.width(), row.height());
                grid.square(i, j).setRgb(medianRgb);
            }
    }

    public void findGridText(List<DocumentText> texts, Rectangle region, GridPosition pos, Grid grid) {
        StringBuilder[][] builders = new StringBuilder[pos.getNumRows()][pos.getNumCols()];
        for (int i = 0; i < pos.getNumRows(); i++)
            for (int j = 0; j < pos.getNumCols(); j++)
                builders[i][j] = new StringBuilder();
        for (DocumentText text : texts) {
            Rectangle r = text.bounds();
            int x = (int) (r.x() + r.width() / 2 - region.x());
            int y = (int) (r.y() - r.height() / 2 - region.y());
            int textRow = -1;
            for (int i = 0; i < pos.getNumRows(); i++) {
                Row row = pos.rows().get(i);
                if (y >= row.startY() && y < row.startY() + row.height())
                    textRow = i;
            }
            int textCol = -1;
            for (int i = 0; i < pos.getNumCols(); i++) {
                Col col = pos.cols().get(i);
                if (x >= col.startX() && x < col.startX() + col.width())
                    textCol = i;
            }
            if (textRow != -1 && textCol != -1)
                builders[textRow][textCol].append(text.text());
        }
        for (int i = 0; i < pos.getNumRows(); i++)
            for (int j = 0; j < pos.getNumCols(); j++)
                grid.square(i, j).setText(builders[i][j].toString());
    }

    public void findGridBorders(BufferedImage image, GridPosition pos, Grid grid) {
        for (int i = 0; i < pos.getNumRows(); i++)
            for (int j = 0; j < pos.getNumCols(); j++) {
                Row row = pos.rows().get(i);
                Col col = pos.cols().get(j);
                Square square = grid.square(i, j);
                square.setTopBorder(ImageUtils.findVerticalBorder(ImageUtils.rotate90DegreesClockwise(image.getSubimage(
                        col.startX(),
                        Math.max(0, row.startY() - row.height() / 2),
                        col.width(),
                        row.height()))));
                square.setRightBorder(ImageUtils.findVerticalBorder(image.getSubimage(
                        Math.min(image.getWidth() - col.width(), col.startX() + col.width() / 2),
                        row.startY(),
                        col.width(),
                        row.height())));
                square.setBottomBorder(ImageUtils.findVerticalBorder(ImageUtils.rotate90DegreesClockwise(image.getSubimage(
                        col.startX(),
                        Math.min(image.getHeight() - row.height(), row.startY() + row.height() / 2),
                        col.width(),
                        row.height()))));
                square.setLeftBorder(ImageUtils.findVerticalBorder(image.getSubimage(
                        Math.max(0, col.startX() - col.width() / 2),
                        row.startY(),
                        col.width(),
                        row.height())));
            }
    }

    public void findGridBorderStyles(Grid grid) {
        List<Tuple> borderWidths = new ArrayList<>();
        for (int i = 0; i < grid.numRows(); i++)
            for (int j = 0; j < grid.numCols(); j++) {
                Square square = grid.square(i, j);
                for (Border border : square.borders())
                    if (border.getWidth() > 0)
                        borderWidths.add(new Tuple(border.getWidth()));
            }

        Clusters clusters = null;
        List<Tuple> centers = new ArrayList<>();
        for (int numClusters = Style.values().length - 1; numClusters >= 1; numClusters--) {
            if (borderWidths.size() < numClusters)
                continue;
            clusters = openCv.findClusters(borderWidths, numClusters);
            centers = clusters.centers();
            double minDistance = Double.MAX_VALUE;
            for (int i = 0; i < centers.size(); i++)
                for (int j = i + 1; j < centers.size(); j++) {
                    double dist = Math.abs(centers.get(i).get(0) - centers.get(j).get(0));
                    if (dist < minDistance)
                        minDistance = dist;
                }
            if (minDistance > MIN_WIDTH_DIFF_BETWEEN_BORDER_STYLES)
                break;
        }

        List<Double> sortedCenters = centers.stream()
                .map(tuple -> tuple.get(0))
                .sorted()
                .toList();

        for (int i = 0; i < grid.numRows(); i++)
            for (int j = 0; j < grid.numCols(); j++) {
                Square square = grid.square(i, j);
                for (Border border : square.borders()) {
                    int width = border.getWidth();
                    int styleLevel;
                    if (width == 0)
                        styleLevel = 0;
                    else {
                        int label = clusters.labels().get(new Tuple(width));
                        double center = centers.get(label).get(0);
                        styleLevel = sortedCenters.indexOf(center) + 1;
                    }
                    border.setStyle(Style.values()[styleLevel]);
                }
            }
    }
}
