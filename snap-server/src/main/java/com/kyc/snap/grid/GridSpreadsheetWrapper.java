package com.kyc.snap.grid;

import java.util.ArrayList;
import java.util.List;

import com.kyc.snap.google.SpreadsheetManager;
import com.kyc.snap.google.SpreadsheetManager.BorderedCell;
import com.kyc.snap.google.SpreadsheetManager.ColoredCell;
import com.kyc.snap.google.SpreadsheetManager.ValueCell;
import com.kyc.snap.grid.Grid.Square;

import lombok.Data;

@Data
public class GridSpreadsheetWrapper {

    private final SpreadsheetManager spreadsheets;

    public void toSpreadsheet(Grid grid) {
        spreadsheets.clear();
        spreadsheets.setAllColumnWidths(20);

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
