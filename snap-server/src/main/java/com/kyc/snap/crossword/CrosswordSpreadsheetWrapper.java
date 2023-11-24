
package com.kyc.snap.crossword;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.kyc.snap.crossword.CrosswordClues.ClueSection;
import com.kyc.snap.crossword.CrosswordClues.NumberedClue;
import com.kyc.snap.google.SpreadsheetManager;
import com.kyc.snap.google.SpreadsheetManager.Dimension;
import com.kyc.snap.google.SpreadsheetManager.SizedRowOrColumn;
import com.kyc.snap.google.SpreadsheetManager.ValueCell;
import com.kyc.snap.grid.Grid;

public record CrosswordSpreadsheetWrapper(SpreadsheetManager spreadsheets, int rowOffset, int colOffset) {

    private static final int[] DROW = {0, 1};
    private static final int[] DCOL = {1, 0};
    private static final String MONOSPACE_FONT = "Roboto Mono";

    public void toSpreadsheet(Grid grid, Crossword crossword, CrosswordClues clues) {
        List<Integer> directionColumns = new ArrayList<>();
        for (int i = 0; i < clues.sections().size(); i++)
            directionColumns.add(grid.numCols() + 1 + 4 * i + colOffset);

        Multimap<ClueKey, Crossword.Entry> crosswordEntries = Multimaps.index(crossword.entries(),
                entry -> new ClueKey(entry.direction(), entry.clueNumber()));
        Multimap<Point, PointAndIndex> gridToAnswers = ArrayListMultimap.create();

        List<ValueCell> valueCells = new ArrayList<>();
        List<ValueCell> formulaCells = new ArrayList<>();
        for (int i = 0; i < clues.sections().size(); i++) {
            ClueSection section = clues.sections().get(i);
            for (int j = 0; j < section.clues().size(); j++) {
                NumberedClue clue = section.clues().get(j);
                valueCells.add(new ValueCell(j + rowOffset, directionColumns.get(i), clue.clue().trim()));

                Crossword.Entry crosswordEntry = Iterables
                        .getOnlyElement(crosswordEntries.get(new ClueKey(section.direction(), clue.clueNumber())));
                List<String> gridRefs = new ArrayList<>();
                for (int k = 0; k < crosswordEntry.numSquares(); k++) {
                    int row = crosswordEntry.startRow() + k * DROW[i] + rowOffset;
                    int col = crosswordEntry.startCol() + k * DCOL[i] + colOffset;
                    gridRefs.add(String.format(
                            "IF(%1$s=\"\", \".\", %1$s)",
                            spreadsheets.getRef(row, col)));
                    gridToAnswers.put(new Point(col, row), new PointAndIndex(j + rowOffset, directionColumns.get(i) + 2, k));
                }
                formulaCells.add(new ValueCell(
                        j + rowOffset,
                        directionColumns.get(i) + 1,
                        String.format(
                                "=CONCATENATE(%s, \" (%d)\")",
                                Joiner.on(',').join(gridRefs),
                                crosswordEntry.numSquares())));
            }
        }

        Function<PointAndIndex, String> nthCharFormula = answer -> String.format(
                // the regex expression takes the nth character of a given string, treating (...) as a single character
                "CONCATENATE(REGEXEXTRACT(%s,\"(?:[^\\(\\)]|\\([^\\)]*\\)){%d}(?:([^\\(\\)])|\\(([^\\)]*)\\))\"))",
                spreadsheets.getRef(answer.row, answer.col),
                answer.index);
        for (Point p : gridToAnswers.keySet()) {
            Collection<PointAndIndex> answers = gridToAnswers.get(p);
            String refArray = answers.stream()
                    .map(nthCharFormula)
                    .collect(Collectors.joining(";"));
            String filterArray = answers.stream()
                    .map(answer -> nthCharFormula.apply(answer) + "<>\"\"")
                    .collect(Collectors.joining(";"));
            String allCharsExpression = String.format(
                    "UNIQUE(FILTER({%s},{%s}))",
                    refArray,
                    filterArray);
            formulaCells.add(new ValueCell(
                    p.y,
                    p.x,
                    String.format(
                            "=IFERROR(IF(COUNTA(%1$s)>1,CONCATENATE(\"[\",JOIN(\"/\",%1$s),\"]\"),%1$s),\"\")",
                            allCharsExpression)));
        }

        spreadsheets.setFont(rowOffset, grid.numRows(), colOffset, grid.numCols(), MONOSPACE_FONT);
        spreadsheets.setTextAlignment(rowOffset, grid.numRows(), colOffset, grid.numCols(), "CENTER", "MIDDLE");
        spreadsheets.setProtectedRange(rowOffset, grid.numRows(), colOffset, grid.numCols());
        for (int i = 0; i < clues.sections().size(); i++) {
            int numRows = clues.sections().get(i).clues().size();
            if (numRows > 0) {
                spreadsheets.setProtectedRange(0, numRows, directionColumns.get(i), 2);
                spreadsheets.setFont(0, numRows, directionColumns.get(i) + 1, 2, MONOSPACE_FONT);
            }
        }

        spreadsheets.setValues(valueCells);
        spreadsheets.setFormulas(formulaCells);

        for (int directionColumn : directionColumns) {
            spreadsheets.setAutomaticRowOrColumnSizes(Dimension.COLUMNS, List.of(directionColumn));
            spreadsheets.setRowOrColumnSizes(Dimension.COLUMNS, List.of(
                    new SizedRowOrColumn(directionColumn + 1, 100),
                    new SizedRowOrColumn(directionColumn + 2, 100),
                    new SizedRowOrColumn(directionColumn + 3, 50)));
        }
    }

    private record ClueKey(ClueDirection direction, int clueNumber) {}

    private record PointAndIndex(int row, int col, int index) {}
}
