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

    public static final int DEFAULT_CELL_LENGTH = 21;
    public static final double DEFAULT_SCALE = 0.75;

    private final SpreadsheetManager spreadsheets;

    public void toSpreadsheet(GridPosition pos, Grid grid) {
        spreadsheets.clear();
        spreadsheets.setAllRowOrColumnSizes(Dimension.ROWS, DEFAULT_CELL_LENGTH);
        spreadsheets.setAllRowOrColumnSizes(Dimension.COLUMNS, DEFAULT_CELL_LENGTH);

        List<SizedRowOrColumn> rows = new ArrayList<>();
        for (int i = 0; i < pos.getNumRows(); i++)
            rows.add(new SizedRowOrColumn(i, (int) (pos.getRows().get(i).getHeight() * DEFAULT_SCALE)));
        spreadsheets.setRowOrColumnSizes(Dimension.ROWS, rows);

        List<SizedRowOrColumn> cols = new ArrayList<>();
        for (int j = 0; j < pos.getNumCols(); j++)
            cols.add(new SizedRowOrColumn(j, (int) (pos.getCols().get(j).getWidth() * DEFAULT_SCALE)));
        spreadsheets.setRowOrColumnSizes(Dimension.COLUMNS, cols);

        List<ColoredCell> coloredCells = new ArrayList<>();
        List<ValueCell> valueCells = new ArrayList<>();
        List<BorderedCell> borderedCells = new ArrayList<>();
        for (int i = 0; i < grid.getNumRows(); i++)
            for (int j = 0; j < grid.getNumCols(); j++) {
                Square square = grid.square(i, j);
                coloredCells.add(new ColoredCell(i, j, square.getRgb()));
                valueCells.add(new ValueCell(i, j, square.getText().trim()));
                borderedCells.add(new BorderedCell(i, j, square.getRightBorder(), square.getBottomBorder()));
            }
        spreadsheets.setBackgroundColors(coloredCells);
        spreadsheets.setValues(valueCells);
        spreadsheets.setBorders(borderedCells);
    }
}
