package com.kyc.snap.grid;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.google.common.base.Preconditions;
import com.kyc.snap.google.GoogleAPIManager;
import com.kyc.snap.grid.Grid.Square;
import com.kyc.snap.grid.GridPosition.Col;
import com.kyc.snap.grid.GridPosition.Row;
import com.kyc.snap.image.ImageUtils;
import com.kyc.snap.opencv.OpenCvManager;
import com.kyc.snap.opencv.OpenCvManager.Line;
import com.kyc.snap.util.Utils;

import lombok.Data;

@Data
public class GridParser {

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

    public void setGridColors(BufferedImage image, GridPosition pos, Grid grid) {
        for (int i = 0; i < pos.getNumRows(); i++)
            for (int j = 0; j < pos.getNumCols(); j++) {
                Row row = pos.getRows().get(i);
                Col col = pos.getCols().get(j);
                int medianRgb = ImageUtils.medianRgb(image, col.getStartX(), row.getStartY(), col.getWidth(), row.getHeight());
                grid.square(i, j).setRgb(medianRgb);
            }
    }

    public void setGridText(BufferedImage image, GridPosition pos, Grid grid) {
        Map<GridLocation, BufferedImage> subimages = new HashMap<>();
        for (Row row : pos.getRows())
            for (Col col : pos.getCols()) {
                subimages.put(
                    new GridLocation(row, col),
                    image.getSubimage(col.getStartX(), row.getStartY(), col.getWidth(), row.getHeight()));
            }
        Map<BufferedImage, String> allText = googleApi.batchFindText(subimages.values());
        for (int i = 0; i < pos.getNumRows(); i++)
            for (int j = 0; j < pos.getNumCols(); j++) {
                Row row = pos.getRows().get(i);
                Col col = pos.getCols().get(j);
                String text = allText.get(subimages.get(new GridLocation(row, col)));
                grid.square(i, j).setText(text);
            }
    }

    public void setGridBorders(BufferedImage image, GridPosition pos, Grid grid) {
        for (int i = 0; i < pos.getNumRows(); i++)
            for (int j = 0; j < pos.getNumCols(); j++) {
                Row row = pos.getRows().get(i);
                Col col = pos.getCols().get(j);
                Square square = grid.square(i, j);
                if (j < pos.getNumCols() - 1) {
                    Border rightBorder = ImageUtils.findVerticalBorder(image.getSubimage(
                        col.getStartX() + col.getWidth() * 3 / 4,
                        row.getStartY() + row.getHeight() / 4,
                        col.getWidth() / 4 + pos.getCols().get(j + 1).getWidth() / 4,
                        row.getHeight() / 2));
                    square.setRightBorder(rightBorder);
                }
                if (i < pos.getNumRows() - 1) {
                    Border bottomBorder = ImageUtils.findVerticalBorder(ImageUtils.rotate90DegreesClockwise(image.getSubimage(
                        col.getStartX() + col.getWidth() / 4,
                        row.getStartY() + 3 * row.getHeight() / 4,
                        col.getWidth() / 2,
                        row.getHeight() / 4 + pos.getRows().get(i + 1).getHeight() / 4)));
                    square.setBottomBorder(bottomBorder);
                }
            }
    }

    @Data
    private static class GridLocation {

        private final Row row;
        private final Col col;
    }
}
