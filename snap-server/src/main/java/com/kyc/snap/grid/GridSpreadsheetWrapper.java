package com.kyc.snap.grid;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.kyc.snap.google.SpreadsheetManager;
import com.kyc.snap.google.SpreadsheetManager.BorderedCell;
import com.kyc.snap.google.SpreadsheetManager.ColoredCell;
import com.kyc.snap.google.SpreadsheetManager.Dimension;
import com.kyc.snap.google.SpreadsheetManager.SizedRowOrColumn;
import com.kyc.snap.google.SpreadsheetManager.ValueCell;
import com.kyc.snap.grid.Grid.Square;
import com.kyc.snap.image.ImageUtils;

import lombok.Data;

@Data
public class GridSpreadsheetWrapper {

    public static final int AVERAGE_HEIGHT = 25;
    public static final double IMAGE_OPACITY = 0.4;
    /**
     * For some reason, a column of width X pixels is slightly longer than a row of height X pixels.
     * This value represents the correction ratio.
     */
    public static final double WIDTH_CORRECTION = 1.04;

    private final SpreadsheetManager spreadsheets;
    private final int rowOffset;
    private final int colOffset;

    public void toSpreadsheet(GridPosition pos, Grid grid, BufferedImage image) {
        int totalWidth = pos.getCols().stream().mapToInt(GridPosition.Col::getWidth).sum();
        int totalHeight = pos.getRows().stream().mapToInt(GridPosition.Row::getHeight).sum();
        double scale = (double) pos.getRows().size() * AVERAGE_HEIGHT / totalHeight;

        List<SizedRowOrColumn> rows = new ArrayList<>();
        for (int i = 0; i < pos.getNumRows(); i++)
            rows.add(new SizedRowOrColumn(i + rowOffset, (int) (pos.getRows().get(i).getHeight() * scale)));
        spreadsheets.setRowOrColumnSizes(Dimension.ROWS, rows);

        List<SizedRowOrColumn> cols = new ArrayList<>();
        for (int j = 0; j < pos.getNumCols(); j++)
            cols.add(new SizedRowOrColumn(j + colOffset, (int) (pos.getCols().get(j).getWidth() * scale)));
        spreadsheets.setRowOrColumnSizes(Dimension.COLUMNS, cols);

        List<ColoredCell> coloredCells = new ArrayList<>();
        List<ValueCell> valueCells = new ArrayList<>();
        List<BorderedCell> borderedCells = new ArrayList<>();
        for (int i = 0; i < grid.getNumRows(); i++)
            for (int j = 0; j < grid.getNumCols(); j++) {
                Square square = grid.square(i, j);
                coloredCells.add(new ColoredCell(i + rowOffset, j + colOffset, square.getRgb()));
                valueCells.add(new ValueCell(i + rowOffset, j + colOffset, square.getText().trim()));
                borderedCells.add(new BorderedCell(
                    i + rowOffset,
                    j + colOffset,
                    square.getTopBorder(),
                    square.getRightBorder(),
                    square.getBottomBorder(),
                    square.getLeftBorder()));
            }
        spreadsheets.setBackgroundColors(coloredCells);
        spreadsheets.setValues(valueCells);
        spreadsheets.setBorders(borderedCells);

        image = image.getSubimage(pos.getCols().get(0).getStartX(), pos.getRows().get(0).getStartY(), totalWidth, totalHeight);
        image = ImageUtils.makeTransparent(image, IMAGE_OPACITY);
        spreadsheets.insertImage(
            image,
            rowOffset,
            colOffset,
            (int) Math.round(cols.stream().mapToInt(SizedRowOrColumn::getSize).sum() * WIDTH_CORRECTION),
            rows.stream().mapToInt(SizedRowOrColumn::getSize).sum(),
            0,
            0);
    }
}
