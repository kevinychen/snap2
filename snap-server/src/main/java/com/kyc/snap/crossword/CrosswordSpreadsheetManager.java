
package com.kyc.snap.crossword;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.kyc.snap.crossword.CrosswordClues.Clue;
import com.kyc.snap.google.GoogleAPIManager;
import com.kyc.snap.google.SpreadsheetManager;
import com.kyc.snap.google.SpreadsheetManager.ColoredCell;
import com.kyc.snap.google.SpreadsheetManager.ValueCell;
import com.kyc.snap.grid.Grid;

import lombok.Data;

@Data
public class CrosswordSpreadsheetManager {

    private final GoogleAPIManager googleApi;

    public void toSpreadsheet(Grid grid, Crossword crossword, CrosswordClues clues) {
        SpreadsheetManager spreadsheets = googleApi.getSheet(GoogleAPIManager.SAMPLE_SPREADSHEET_ID, GoogleAPIManager.SAMPLE_SHEET_ID);

        spreadsheets.clear();
        spreadsheets.setAllColumnWidths(20);

        List<ColoredCell> coloredCells = new ArrayList<>();
        for (int i = 0; i < grid.getNumRows(); i++)
            for (int j = 0; j < grid.getNumCols(); j++)
                coloredCells.add(new ColoredCell(i, j, grid.getSquare(i, j).getRgb()));
        spreadsheets.setBackgroundColors(coloredCells);

        ClueDirection[] directions = { ClueDirection.ACROSS, ClueDirection.DOWN };
        List<Integer> directionColumns = new ArrayList<>();
        for (int i = 0; i < directions.length; i++)
            directionColumns.add(grid.getNumCols() + 1 + 4 * i);

        Multimap<ClueDirection, Clue> cluesByDirection = Multimaps.index(clues.getClues(), clue -> clue.getDirection());
        List<ValueCell> valueCells = new ArrayList<>();
        for (int i = 0; i < directions.length; i++) {
            List<Clue> cluesForDirection = cluesByDirection.get(directions[i]).stream()
                    .sorted(Comparator.comparing(clue -> clue.getClueNumber()))
                    .collect(Collectors.toList());
            for (int j = 0; j < cluesForDirection.size(); j++)
                valueCells.add(new ValueCell(j, directionColumns.get(i), cluesForDirection.get(j).getClue()));
        }
        spreadsheets.setValues(valueCells);

        for (int directionColumn : directionColumns)
            spreadsheets.setAutomaticColumnWidths(directionColumn, directionColumn + 1);
    }
}
