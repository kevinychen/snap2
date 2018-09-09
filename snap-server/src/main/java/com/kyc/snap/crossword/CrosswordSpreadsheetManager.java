
package com.kyc.snap.crossword;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
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

    private static final int ROW_OFFSET = 1;
    private static final int COL_OFFSET = 1;
    private static final int UNKNOWN_MARKER_ROW = 128;
    private static final int UNKNOWN_MARKER_COL = 128;
    private static final ClueDirection[] DIRECTIONS = { ClueDirection.ACROSS, ClueDirection.DOWN };
    private static final int[] DROW = { 0, 1 };
    private static final int[] DCOL = { 1, 0 };

    private final GoogleAPIManager googleApi;

    public void toSpreadsheet(Grid grid, Crossword crossword, CrosswordClues clues) {
        SpreadsheetManager spreadsheets = googleApi.getSheet(GoogleAPIManager.SAMPLE_SPREADSHEET_ID, GoogleAPIManager.SAMPLE_SHEET_ID);

        spreadsheets.clear();
        spreadsheets.setAllColumnWidths(20);

        List<ColoredCell> coloredCells = new ArrayList<>();
        for (int i = 0; i < grid.getNumRows(); i++)
            for (int j = 0; j < grid.getNumCols(); j++)
                coloredCells.add(new ColoredCell(i + ROW_OFFSET, j + COL_OFFSET, grid.getSquare(i, j).getRgb()));
        spreadsheets.setBackgroundColors(coloredCells);

        List<Integer> directionColumns = new ArrayList<>();
        for (int i = 0; i < DIRECTIONS.length; i++)
            directionColumns.add(grid.getNumCols() + 1 + 4 * i);

        Multimap<ClueDirection, Clue> cluesByDirection = Multimaps.index(clues.getClues(), clue -> clue.getDirection());
        Multimap<ClueKey, Crossword.Entry> crosswordEntries = Multimaps.index(crossword.getEntries(),
            entry -> new ClueKey(entry.getDirection(), entry.getClueNumber()));
        Multimap<Square, SquareIndex> gridToAnswers = ArrayListMultimap.create();

        List<ValueCell> valueCells = new ArrayList<>();
        valueCells.add(new ValueCell(UNKNOWN_MARKER_ROW, UNKNOWN_MARKER_COL, "."));
        List<ValueCell> formulaCells = new ArrayList<>();
        for (int i = 0; i < DIRECTIONS.length; i++) {
            List<Clue> cluesForDirection = cluesByDirection.get(DIRECTIONS[i]).stream()
                .sorted(Comparator.comparing(clue -> clue.getClueNumber()))
                .collect(Collectors.toList());
            for (int j = 0; j < cluesForDirection.size(); j++) {
                Clue clue = cluesForDirection.get(j);
                valueCells.add(new ValueCell(j + ROW_OFFSET, directionColumns.get(i) + COL_OFFSET, clue.getClue()));

                Crossword.Entry crosswordEntry = Iterables
                    .getOnlyElement(crosswordEntries.get(new ClueKey(clue.getDirection(), clue.getClueNumber())));
                List<String> gridRefs = new ArrayList<>();
                for (int k = 0; k < crosswordEntry.getNumSquares(); k++) {
                    int row = crosswordEntry.getStartRow() + k * DROW[i];
                    int col = crosswordEntry.getStartCol() + k * DCOL[i];
                    gridRefs.add(String.format(
                        "IF(%1$s=\"\", %2$s, %1$s)",
                        spreadsheets.getRef(row + ROW_OFFSET, col + COL_OFFSET),
                        spreadsheets.getRef(UNKNOWN_MARKER_ROW, UNKNOWN_MARKER_COL)));
                    gridToAnswers.put(new Square(row, col), new SquareIndex(j, directionColumns.get(i) + 2, k));
                }
                formulaCells.add(new ValueCell(
                    j + ROW_OFFSET,
                    directionColumns.get(i) + 1 + COL_OFFSET,
                    String.format(
                        "=CONCATENATE(%s, \" (%d)\")",
                        Joiner.on(',').join(gridRefs),
                        crosswordEntry.getNumSquares())));
            }
        }

        for (Square gridSquare : gridToAnswers.keySet()) {
            Collection<SquareIndex> answers = gridToAnswers.get(gridSquare);
            String refArray = Joiner.on(";").join(answers.stream()
                .map(answer -> String.format(
                    "MID(%s,%d,1)",
                    spreadsheets.getRef(answer.row + ROW_OFFSET, answer.col + COL_OFFSET),
                    answer.index + 1))
                .collect(Collectors.toList()));
            String filterArray = Joiner.on(";").join(answers.stream()
                .map(answer -> String.format(
                    "MID(%s,%d,1)<>\"\"",
                    spreadsheets.getRef(answer.row + ROW_OFFSET, answer.col + COL_OFFSET),
                    answer.index + 1))
                .collect(Collectors.toList()));
            String allCharsExpression = String.format(
                "UNIQUE(FILTER({%s},{%s}))",
                refArray,
                filterArray);
            formulaCells.add(new ValueCell(
                gridSquare.row + ROW_OFFSET,
                gridSquare.col + COL_OFFSET,
                String.format("=IFERROR(IF(COUNTA(%1$s)>1,CONCATENATE(\"[\",JOIN(\"/\",%1$s),\"]\"),%1$s),\"\")",
                    allCharsExpression)));
        }

        spreadsheets.setValues(valueCells);
        spreadsheets.setFormulas(formulaCells);

        for (int directionColumn : directionColumns) {
            spreadsheets.setAutomaticColumnWidths(directionColumn + COL_OFFSET, directionColumn + 1 + COL_OFFSET);
            spreadsheets.setColumnWidths(100, directionColumn + 1 + COL_OFFSET, directionColumn + 3 + COL_OFFSET);
        }
    }

    @Data
    private static class ClueKey {

        private final ClueDirection direction;
        private final int clueNumber;
    }

    @Data
    private static class Square {

        private final int row;
        private final int col;
    }

    @Data
    private static class SquareIndex {

        private final int row;
        private final int col;
        private final int index;
    }
}
