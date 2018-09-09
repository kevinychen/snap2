package com.kyc.snap.crossword;

import java.util.ArrayList;
import java.util.List;

import com.kyc.snap.google.GoogleAPIManager;
import com.kyc.snap.google.SpreadsheetManager;
import com.kyc.snap.google.SpreadsheetManager.ColoredCell;
import com.kyc.snap.grid.Grid;

import lombok.Data;

@Data
public class CrosswordSpreadsheetManager {

    private final GoogleAPIManager googleApi;

    public void toSpreadsheet(Grid grid, Crossword crossword, CrosswordClues clues) {
        SpreadsheetManager spreadsheets = googleApi.getSheet(GoogleAPIManager.SAMPLE_SPREADSHEET_ID, GoogleAPIManager.SAMPLE_SHEET_ID);

        spreadsheets.makeEmptyGrid();

        List<ColoredCell> cells = new ArrayList<>();
        for (int i = 0; i < grid.getNumRows(); i++)
            for (int j = 0; j < grid.getNumCols(); j++)
                cells.add(new ColoredCell(i, j, grid.getSquare(i, j).getRgb()));
        spreadsheets.setBackgroundColor(cells);
    }
}
