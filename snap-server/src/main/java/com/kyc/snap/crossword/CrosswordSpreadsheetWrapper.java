
package com.kyc.snap.crossword;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
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

import lombok.Data;

@Data
public class CrosswordSpreadsheetWrapper {

    private static final int BLANK_MARKER_ROW = 500;
    private static final int UNKNOWN_MARKER_ROW = 501;
    private static final int MARKER_COL = 25;
    private static final int[] DROW = { 0, 1 };
    private static final int[] DCOL = { 1, 0 };

    private final SpreadsheetManager spreadsheets;
    private final int rowOffset;
    private final int colOffset;

    public void toSpreadsheet(Grid grid, Crossword crossword, CrosswordClues clues) {
        List<Integer> directionColumns = new ArrayList<>();
        for (int i = 0; i < clues.getSections().size(); i++)
            directionColumns.add(grid.getNumCols() + 1 + 4 * i + colOffset);

        Multimap<ClueKey, Crossword.Entry> crosswordEntries = Multimaps.index(crossword.getEntries(),
            entry -> new ClueKey(entry.getDirection(), entry.getClueNumber()));
        Multimap<Point, PointAndIndex> gridToAnswers = ArrayListMultimap.create();

        List<ValueCell> valueCells = new ArrayList<>();
        valueCells.add(new ValueCell(UNKNOWN_MARKER_ROW, MARKER_COL, "."));
        List<ValueCell> formulaCells = new ArrayList<>();
        for (int i = 0; i < clues.getSections().size(); i++) {
            ClueSection section = clues.getSections().get(i);
            for (int j = 0; j < section.getClues().size(); j++) {
                NumberedClue clue = section.getClues().get(j);
                valueCells.add(new ValueCell(j + rowOffset, directionColumns.get(i), clue.getClue().trim()));

                Crossword.Entry crosswordEntry = Iterables
                    .getOnlyElement(crosswordEntries.get(new ClueKey(section.getDirection(), clue.getClueNumber())));
                List<String> gridRefs = new ArrayList<>();
                for (int k = 0; k < crosswordEntry.getNumSquares(); k++) {
                    int row = crosswordEntry.getStartRow() + k * DROW[i] + rowOffset;
                    int col = crosswordEntry.getStartCol() + k * DCOL[i] + colOffset;
                    gridRefs.add(String.format(
                        "IF(%1$s=%2$s, %3$s, %1$s)",
                        spreadsheets.getRef(row, col),
                        spreadsheets.getRef(BLANK_MARKER_ROW, MARKER_COL),
                        spreadsheets.getRef(UNKNOWN_MARKER_ROW, MARKER_COL)));
                    gridToAnswers.put(new Point(col, row), new PointAndIndex(j + rowOffset, directionColumns.get(i) + 2, k));
                }
                formulaCells.add(new ValueCell(
                    j + rowOffset,
                    directionColumns.get(i) + 1,
                    String.format(
                        "=CONCATENATE(%s, \" (%d)\")",
                        Joiner.on(',').join(gridRefs),
                        crosswordEntry.getNumSquares())));
            }
        }

        Map<Point, String> references = spreadsheets.getReferences(gridToAnswers.keySet());
        for (Point p : gridToAnswers.keySet()) {
            Collection<PointAndIndex> answers = gridToAnswers.get(p);
            String refArray = Joiner.on(";").join(answers.stream()
                .map(answer -> String.format(
                    "MID(%s,%d,1)",
                    spreadsheets.getRef(answer.row, answer.col),
                    answer.index + 1))
                .collect(Collectors.toList()));
            String filterArray = Joiner.on(";").join(answers.stream()
                .map(answer -> String.format(
                    "MID(%s,%d,1)<>\"\"",
                    spreadsheets.getRef(answer.row, answer.col),
                    answer.index + 1))
                .collect(Collectors.toList()));
            String allCharsExpression = String.format(
                "UNIQUE(FILTER({%s},{%s}))",
                refArray,
                filterArray);
            formulaCells.add(new ValueCell(
                p.y,
                p.x,
                String.format("=IFERROR(IF(COUNTA(%1$s)>1,CONCATENATE(\"[\",JOIN(\"/\",%1$s),\"]\"),%1$s),%2$s)",
                    allCharsExpression,
                    references.get(p))));
        }

        spreadsheets.setValues(valueCells);
        spreadsheets.setFormulas(formulaCells);

        for (int directionColumn : directionColumns) {
            spreadsheets.setAutomaticRowOrColumnSizes(Dimension.COLUMNS, ImmutableList.of(directionColumn));
            spreadsheets.setRowOrColumnSizes(Dimension.COLUMNS, ImmutableList.of(
                new SizedRowOrColumn(directionColumn + 1, 100),
                new SizedRowOrColumn(directionColumn + 2, 100),
                new SizedRowOrColumn(directionColumn + 3, 50)));
        }
    }

    @Data
    private static class ClueKey {

        private final ClueDirection direction;
        private final int clueNumber;
    }

    @Data
    private static class PointAndIndex {

        private final int row;
        private final int col;
        private final int index;
    }
}
