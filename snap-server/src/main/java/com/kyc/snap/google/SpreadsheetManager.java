
package com.kyc.snap.google;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.google.api.services.sheets.v4.Sheets.Spreadsheets;
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

    public void makeEmptyGrid() {
        try {
            spreadsheets.batchUpdate(spreadsheet.getSpreadsheetId(), new BatchUpdateSpreadsheetRequest()
                .setRequests(ImmutableList.of(new Request()
                    .setUpdateCells(new UpdateCellsRequest()
                        .setFields("userEnteredFormat,userEnteredValue")
                        .setRange(new GridRange()
                            .setSheetId(sheetId))),
                    new Request()
                        .setUpdateDimensionProperties(new UpdateDimensionPropertiesRequest()
                            .setProperties(new DimensionProperties()
                                .setPixelSize(20))
                            .setFields("pixelSize")
                            .setRange(new DimensionRange()
                                .setSheetId(sheetId)
                                .setDimension("COLUMNS"))))))
                .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setBackgroundColors(List<ColoredCell> cells) {
        try {
            spreadsheets.batchUpdate(spreadsheet.getSpreadsheetId(), new BatchUpdateSpreadsheetRequest()
                .setRequests(cells.stream()
                    .map(cell -> new Request()
                        .setUpdateCells(new UpdateCellsRequest()
                            .setRows(ImmutableList.of(new RowData()
                                .setValues(ImmutableList.of(new CellData()
                                    .setUserEnteredFormat(new CellFormat()
                                        .setBackgroundColor(toColor(cell.rgb)))))))
                            .setFields("userEnteredFormat.backgroundColor")
                            .setRange(getRange(cell.row, cell.col))))
                    .collect(Collectors.toList())))
                .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setValues(List<ValueCell> cells) {
        try {
            spreadsheets.batchUpdate(spreadsheet.getSpreadsheetId(), new BatchUpdateSpreadsheetRequest()
                .setRequests(cells.stream()
                    .map(cell -> new Request()
                        .setUpdateCells(new UpdateCellsRequest()
                            .setRows(ImmutableList.of(new RowData()
                                .setValues(ImmutableList.of(new CellData()
                                    .setUserEnteredValue(new ExtendedValue()
                                        .setStringValue(cell.text))))))
                            .setFields("userEnteredValue.stringValue")
                            .setRange(getRange(cell.row, cell.col))))
                    .collect(Collectors.toList())))
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
