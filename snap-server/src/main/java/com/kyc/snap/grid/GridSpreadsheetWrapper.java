package com.kyc.snap.grid;

import java.util.ArrayList;
import java.util.List;

import com.kyc.snap.google.SpreadsheetManager;
import com.kyc.snap.google.SpreadsheetManager.BorderedCell;
import com.kyc.snap.google.SpreadsheetManager.ColoredCell;
import com.kyc.snap.google.SpreadsheetManager.Dimension;
import com.kyc.snap.google.SpreadsheetManager.SizedRowOrColumn;
import com.kyc.snap.google.SpreadsheetManager.ValueCell;
import com.kyc.snap.grid.Grid.Square;

import lombok.Data;

@Data
public class GridSpreadsheetWrapper {

    public static final double DEFAULT_SCALE = 0.75;

    private final SpreadsheetManager spreadsheets;
    private final int rowOffset;
    private final int colOffset;

    public void toSpreadsheet(GridPosition pos, Grid grid, double scale) {
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
    }
}
