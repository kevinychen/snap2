package com.kyc.snap.grid;

import java.util.ArrayList;
import java.util.List;

import com.kyc.snap.google.SpreadsheetManager;
import com.kyc.snap.google.SpreadsheetManager.BorderedCell;
import com.kyc.snap.google.SpreadsheetManager.ColoredCell;
import com.kyc.snap.google.SpreadsheetManager.Dimension;
import com.kyc.snap.google.SpreadsheetManager.SheetData;
import com.kyc.snap.google.SpreadsheetManager.SizedRowOrColumn;
import com.kyc.snap.google.SpreadsheetManager.ValueCell;
import com.kyc.snap.grid.Grid.Square;

public record GridSpreadsheetWrapper(SpreadsheetManager spreadsheets, int rowOffset, int colOffset) {

    public static final int MIN_CELL_SIZE = 25;
    /**
     * If we're inserting a crossword, we need some extra columns for the clues.
     */
    public static final int EXTRA_COLS_BUFFER = 14;

    public void toSpreadsheet(GridPosition pos, Grid grid, SheetData sheetData) {
        int numRowsInSheet = sheetData.rowHeights().size();
        int numColsInSheet = sheetData.colWidths().size();
        if (numRowsInSheet < rowOffset + pos.getNumRows())
            spreadsheets.insertRowOrColumns(Dimension.ROWS, numRowsInSheet, rowOffset + pos.getNumRows() - numRowsInSheet);
        if (numColsInSheet < colOffset + pos.getNumCols() + EXTRA_COLS_BUFFER)
            spreadsheets.insertRowOrColumns(Dimension.COLUMNS, numColsInSheet,
                    colOffset + pos.getNumCols() + EXTRA_COLS_BUFFER - numColsInSheet);

        int totalWidth = pos.cols().stream().mapToInt(GridPosition.Col::width).sum();
        int totalHeight = pos.rows().stream().mapToInt(GridPosition.Row::height).sum();
        double scale = Math.max(
                (double) pos.getNumRows() * MIN_CELL_SIZE / totalHeight,
                (double) pos.getNumCols() * MIN_CELL_SIZE / totalWidth);

        List<SizedRowOrColumn> rows = new ArrayList<>();
        for (int i = 0; i < pos.getNumRows(); i++)
            rows.add(new SizedRowOrColumn(i + rowOffset, (int) (pos.rows().get(i).height() * scale)));
        spreadsheets.setRowOrColumnSizes(Dimension.ROWS, rows);

        List<SizedRowOrColumn> cols = new ArrayList<>();
        for (int j = 0; j < pos.getNumCols(); j++)
            cols.add(new SizedRowOrColumn(j + colOffset, (int) (pos.cols().get(j).width() * scale)));
        spreadsheets.setRowOrColumnSizes(Dimension.COLUMNS, cols);

        List<ColoredCell> coloredCells = new ArrayList<>();
        List<ValueCell> valueCells = new ArrayList<>();
        List<BorderedCell> borderedCells = new ArrayList<>();
        for (int i = 0; i < grid.numRows(); i++)
            for (int j = 0; j < grid.numCols(); j++) {
                Square square = grid.square(i, j);
                coloredCells.add(new ColoredCell(i + rowOffset, j + colOffset, square.rgb));
                valueCells.add(new ValueCell(i + rowOffset, j + colOffset, square.text.trim()));
                borderedCells.add(new BorderedCell(
                        i + rowOffset,
                        j + colOffset,
                        square.topBorder,
                        square.rightBorder,
                        square.bottomBorder,
                        square.leftBorder));
            }
        spreadsheets.setBackgroundColors(coloredCells);
        spreadsheets.setValues(valueCells);
        spreadsheets.setBorders(borderedCells);
    }
}
