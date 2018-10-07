package com.kyc.snap.grid;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.kyc.snap.document.Document.DocumentText;
import com.kyc.snap.document.Rectangle;
import com.kyc.snap.google.GoogleAPIManager;
import com.kyc.snap.grid.Border.Style;
import com.kyc.snap.grid.Grid.Square;
import com.kyc.snap.grid.GridPosition.Col;
import com.kyc.snap.grid.GridPosition.Row;
import com.kyc.snap.image.ImageUtils;
import com.kyc.snap.image.ImageUtils.Blob;
import com.kyc.snap.opencv.OpenCvManager;
import com.kyc.snap.opencv.OpenCvManager.Clusters;
import com.kyc.snap.opencv.OpenCvManager.Line;
import com.kyc.snap.opencv.OpenCvManager.Tuple;
import com.kyc.snap.util.Utils;

import lombok.Data;

@Data
public class GridParser {

    /**
     * Minimum difference between widths of two borders in order for them to have different styles.
     */
    public static final double MIN_WIDTH_DIFF_BETWEEN_BORDER_STYLES = 1.5;

    private final OpenCvManager openCv;
    private final GoogleAPIManager googleApi;

    public GridLines findGridLines(BufferedImage image, int approxGridSquareSize) {
        List<Line> cardinalLines = openCv.findLines(image, Math.PI / 2, approxGridSquareSize);

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
            Utils.deduplicateCloseMarks(horizontalLines, approxGridSquareSize / 4),
            Utils.deduplicateCloseMarks(verticalLines, approxGridSquareSize / 4));
    }

    public GridLines getInterpolatedGridLines(GridLines lines) {
        return new GridLines(
            Utils.findInterpolatedSequence(lines.getHorizontalLines()),
            Utils.findInterpolatedSequence(lines.getVerticalLines()));
    }

    public GridLines findImplicitGridLines(BufferedImage image) {
        List<Blob> blobs = ImageUtils.findBlobs(image, rgb -> !ImageUtils.isLight(rgb));
        TreeSet<Integer> rows = Utils.findInterpolatedSequence(blobs.stream()
            .map(blob -> blob.getY() + blob.getHeight() / 2)
            .collect(Collectors.toList()));
        TreeSet<Integer> cols = Utils.findInterpolatedSequence(blobs.stream()
            .map(blob -> blob.getX() + blob.getWidth() / 2)
            .collect(Collectors.toList()));

        BiFunction<TreeSet<Integer>, Integer, TreeSet<Integer>> findSurroundingMarksFunction = (marks, limit) -> {
            Preconditions.checkArgument(marks.size() >= 2,
                "Expected at least two marks: %s", marks);
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

    public void findGridColors(BufferedImage image, GridPosition pos, Grid grid) {
        for (int i = 0; i < pos.getNumRows(); i++)
            for (int j = 0; j < pos.getNumCols(); j++) {
                Row row = pos.getRows().get(i);
                Col col = pos.getCols().get(j);
                int medianRgb = ImageUtils.medianRgb(image, col.getStartX(), row.getStartY(), col.getWidth(), row.getHeight());
                grid.square(i, j).setRgb(medianRgb);
            }
    }

    public void findGridText(BufferedImage image, GridPosition pos, Grid grid) {
        Map<Point, BufferedImage> subimages = new HashMap<>();
        for (int i = 0; i < pos.getNumRows(); i++)
            for (int j = 0; j < pos.getNumCols(); j++) {
                Row row = pos.getRows().get(i);
                Col col = pos.getCols().get(j);
                subimages.put(
                    new Point(j, i),
                    image.getSubimage(col.getStartX(), row.getStartY(), col.getWidth(), row.getHeight()));
            }
        Map<BufferedImage, String> allText = googleApi.batchFindText(subimages.values());
        for (int i = 0; i < pos.getNumRows(); i++)
            for (int j = 0; j < pos.getNumCols(); j++) {
                String text = allText.get(subimages.get(new Point(j, i)));
                grid.square(i, j).setText(text);
            }
    }

    public void findGridText(List<DocumentText> texts, Rectangle region, GridPosition pos, Grid grid) {
        StringBuilder[][] builders = new StringBuilder[pos.getNumRows()][pos.getNumCols()];
        for (int i = 0; i < pos.getNumRows(); i++)
            for (int j = 0; j < pos.getNumCols(); j++)
                builders[i][j] = new StringBuilder();
        for (DocumentText text : texts) {
            Rectangle r = text.getBounds();
            int x = (int) (r.getX() + r.getWidth() / 2 - region.getX());
            int y = (int) (r.getY() + r.getHeight() / 2 - region.getY());
            int textRow = -1;
            for (int i = 0; i < pos.getNumRows(); i++) {
                Row row = pos.getRows().get(i);
                if (y >= row.getStartY() && y < row.getStartY() + row.getHeight())
                    textRow = i;
            }
            int textCol = -1;
            for (int i = 0; i < pos.getNumCols(); i++) {
                Col col = pos.getCols().get(i);
                if (x >= col.getStartX() && x < col.getStartX() + col.getWidth())
                    textCol = i;
            }
            if (textRow != -1 && textCol != -1)
                builders[textRow][textCol].append(text.getText());
        }
        for (int i = 0; i < pos.getNumRows(); i++)
            for (int j = 0; j < pos.getNumCols(); j++)
                grid.square(i, j).setText(builders[i][j].toString());
    }

    public void findGridBorders(BufferedImage image, GridPosition pos, Grid grid) {
        for (int i = 0; i < pos.getNumRows(); i++)
            for (int j = 0; j < pos.getNumCols(); j++) {
                Row row = pos.getRows().get(i);
                Col col = pos.getCols().get(j);
                Square square = grid.square(i, j);
                square.setTopBorder(ImageUtils.findVerticalBorder(ImageUtils.rotate90DegreesClockwise(image.getSubimage(
                    col.getStartX(),
                    Math.max(0, row.getStartY() - row.getHeight() / 2),
                    col.getWidth(),
                    row.getHeight()))));
                square.setRightBorder(ImageUtils.findVerticalBorder(image.getSubimage(
                    Math.min(image.getWidth() - col.getWidth(), col.getStartX() + col.getWidth() / 2),
                    row.getStartY(),
                    col.getWidth(),
                    row.getHeight())));
                square.setBottomBorder(ImageUtils.findVerticalBorder(ImageUtils.rotate90DegreesClockwise(image.getSubimage(
                    col.getStartX(),
                    Math.min(image.getHeight() - row.getHeight(), row.getStartY() + row.getHeight() / 2),
                    col.getWidth(),
                    row.getHeight()))));
                square.setLeftBorder(ImageUtils.findVerticalBorder(image.getSubimage(
                    Math.max(0, col.getStartX() - col.getWidth() / 2),
                    row.getStartY(),
                    col.getWidth(),
                    row.getHeight())));
            }
    }

    public void findGridBorderStyles(Grid grid) {
        List<Tuple> borderWidths = new ArrayList<>();
        for (int i = 0; i < grid.getNumRows(); i++)
            for (int j = 0; j < grid.getNumCols(); j++) {
                Square square = grid.square(i, j);
                for (Border border : square.borders())
                    if (border.getWidth() > 0)
                        borderWidths.add(new Tuple(border.getWidth()));
            }

        Clusters clusters = null;
        List<Tuple> centers = null;
        for (int numClusters = Style.values().length - 1; numClusters >= 1; numClusters--) {
            clusters = openCv.findClusters(borderWidths, numClusters);
            centers = clusters.getCenters();
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
                .collect(Collectors.toList());

        for (int i = 0; i < grid.getNumRows(); i++)
            for (int j = 0; j < grid.getNumCols(); j++) {
                Square square = grid.square(i, j);
                for (Border border : square.borders()) {
                    int width = border.getWidth();
                    int styleLevel;
                    if (width == 0)
                        styleLevel = 0;
                    else {
                        int label = clusters.getLabels().get(new Tuple(width));
                        double center = centers.get(label).get(0);
                        styleLevel = sortedCenters.indexOf(center) + 1;
                    }
                    border.setStyle(Style.values()[styleLevel]);
                }
            }
    }
}
