
package com.kyc.snap.google;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.api.services.sheets.v4.Sheets.Spreadsheets;
import com.google.api.services.sheets.v4.model.AutoResizeDimensionsRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.DimensionProperties;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.UpdateCellsRequest;
import com.google.api.services.sheets.v4.model.UpdateDimensionPropertiesRequest;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import lombok.Data;

@Data
public class SpreadsheetManager {

    private final Spreadsheets spreadsheets;
    private final Spreadsheet spreadsheet;
    private final int sheetId;

    public String getUrl() {
        return spreadsheet.getSpreadsheetUrl();
    }

    public String getRef(int row, int col) {
        Preconditions.checkArgument(col < 26 + 26 * 26, "Column too large to compute ref");
        StringBuilder ref = new StringBuilder();
        if (col >= 26)
            ref.append((char) (col / 26 - 1 + 'A'));
        ref.append((char) (col % 26 + 'A'));
        ref.append(row + 1);
        return ref.toString();
    }

    public void clear() {
        executeRequests(
            new Request()
                .setUpdateCells(new UpdateCellsRequest()
                    .setFields("userEnteredFormat,userEnteredValue")
                    .setRange(new GridRange()
                        .setSheetId(sheetId))));
    }

    public void setAllColumnWidths(int width) {
        executeRequests(
            new Request()
                .setUpdateDimensionProperties(new UpdateDimensionPropertiesRequest()
                    .setProperties(new DimensionProperties()
                        .setPixelSize(width))
                    .setFields("pixelSize")
                    .setRange(new DimensionRange()
                        .setSheetId(sheetId)
                        .setDimension("COLUMNS"))));
    }

    public void setColumnWidths(int width, int startIndex, int endIndex) {
        executeRequests(
            new Request()
                .setUpdateDimensionProperties(new UpdateDimensionPropertiesRequest()
                    .setProperties(new DimensionProperties()
                        .setPixelSize(width))
                    .setFields("pixelSize")
                    .setRange(new DimensionRange()
                        .setSheetId(sheetId)
                        .setDimension("COLUMNS")
                        .setStartIndex(startIndex)
                        .setEndIndex(endIndex))));
    }

    public void setAutomaticColumnWidths(int startIndex, int endIndex) {
        executeRequests(
            new Request()
                .setAutoResizeDimensions(new AutoResizeDimensionsRequest()
                    .setDimensions(new DimensionRange()
                        .setSheetId(sheetId)
                        .setDimension("COLUMNS")
                        .setStartIndex(startIndex)
                        .setEndIndex(endIndex))));
    }

    public void setBackgroundColors(List<ColoredCell> cells) {
        executeRequests(cells.stream()
            .map(cell -> new Request()
                .setUpdateCells(new UpdateCellsRequest()
                    .setRows(ImmutableList.of(new RowData()
                        .setValues(ImmutableList.of(new CellData()
                            .setUserEnteredFormat(new CellFormat()
                                .setBackgroundColor(toColor(cell.rgb)))))))
                    .setFields("userEnteredFormat.backgroundColor")
                    .setRange(getRange(cell.row, cell.col))))
            .collect(Collectors.toList()));
    }

    public void setValues(List<ValueCell> cells) {
        executeRequests(cells.stream()
            .map(cell -> new Request()
                .setUpdateCells(new UpdateCellsRequest()
                    .setRows(ImmutableList.of(new RowData()
                        .setValues(ImmutableList.of(new CellData()
                            .setUserEnteredValue(new ExtendedValue()
                                .setStringValue(cell.text))))))
                    .setFields("userEnteredValue.stringValue")
                    .setRange(getRange(cell.row, cell.col))))
            .collect(Collectors.toList()));
    }

    public void setFormulas(List<ValueCell> cells) {
        executeRequests(cells.stream()
            .map(cell -> new Request()
                .setUpdateCells(new UpdateCellsRequest()
                    .setRows(ImmutableList.of(new RowData()
                        .setValues(ImmutableList.of(new CellData()
                            .setUserEnteredValue(new ExtendedValue()
                                .setFormulaValue(cell.text))))))
                    .setFields("userEnteredValue.formulaValue")
                    .setRange(getRange(cell.row, cell.col))))
            .collect(Collectors.toList()));
    }

    private void executeRequests(Request... requests) {
        executeRequests(Arrays.asList(requests));
    }

    private void executeRequests(List<Request> requests) {
        try {
            spreadsheets
                .batchUpdate(spreadsheet.getSpreadsheetId(), new BatchUpdateSpreadsheetRequest().setRequests(requests))
                .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private GridRange getRange(int row, int col) {
        return new GridRange()
            .setSheetId(sheetId)
            .setStartRowIndex(row)
            .setEndRowIndex(row + 1)
            .setStartColumnIndex(col)
            .setEndColumnIndex(col + 1);
    }

    private static Color toColor(int rgb) {
        java.awt.Color color = new java.awt.Color(rgb);
        return new Color()
            .setRed(color.getRed() / 255f)
            .setGreen(color.getGreen() / 255f)
            .setBlue(color.getBlue() / 255f);
    }

    @Data
    public static class ColoredCell {

        private final int row;
        private final int col;
        private final int rgb;
    }

    @Data
    public static class ValueCell {

        private final int row;
        private final int col;
        private final String text;
    }
}
