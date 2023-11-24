
package com.kyc.snap.crossword;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.kyc.snap.google.SpreadsheetManager;
import com.kyc.snap.google.SpreadsheetManager.Dimension;
import com.kyc.snap.google.SpreadsheetManager.SizedRowOrColumn;
import com.kyc.snap.google.SpreadsheetManager.ValueCell;
import com.kyc.snap.grid.Grid;

public record CrosswordSpreadsheetWrapper(SpreadsheetManager spreadsheets, int rowOffset, int colOffset) {

    private static final String MONOSPACE_FONT = "Roboto Mono";
    private static final Pattern RELATIVE_REFERENCE_PATTERN = Pattern.compile("R\\[([^]]+)]C\\[([^]]+)]");

    public void toSpreadsheet(Grid grid, CrosswordClues clues, List<CrosswordFormula> formulas) {
        List<Integer> directionColumns = new ArrayList<>();
        for (int i = 0; i < clues.sections().size(); i++)
            directionColumns.add(grid.numCols() + 1 + 3 * i + colOffset);

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

        spreadsheets.setValues(formulas.stream()
                .filter(formula -> !formula.formula())
                .map(formula -> new ValueCell(formula.row(), formula.col(), formula.value()))
                .toList());
        spreadsheets.setFormulas(formulas.stream()
                .filter(CrosswordFormula::formula)
                .map(formula -> {
                    String value = formula.value();
                    Matcher matcher = RELATIVE_REFERENCE_PATTERN.matcher(formula.value());
                    while (matcher.find()) {
                        int dRow = Integer.parseInt(matcher.group(1));
                        int dCol = Integer.parseInt(matcher.group(2));
                        value = value.replace(matcher.group(), spreadsheets.getRef(formula.row() + dRow, formula.col() + dCol));
                    }
                    return new ValueCell(formula.row(), formula.col(), value);
                })
                .toList());

        for (int directionColumn : directionColumns) {
            spreadsheets.setAutomaticRowOrColumnSizes(Dimension.COLUMNS, List.of(directionColumn));
            spreadsheets.setRowOrColumnSizes(Dimension.COLUMNS, List.of(
                    new SizedRowOrColumn(directionColumn + 1, 100),
                    new SizedRowOrColumn(directionColumn + 2, 100),
                    new SizedRowOrColumn(directionColumn + 3, 50)));
        }
    }
}
