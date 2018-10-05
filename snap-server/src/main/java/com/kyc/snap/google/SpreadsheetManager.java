
package com.kyc.snap.google;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.ForbiddenException;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets;
import com.google.api.services.sheets.v4.model.AutoResizeDimensionsRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.Border;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.DataFilter;
import com.google.api.services.sheets.v4.model.DimensionProperties;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.api.services.sheets.v4.model.GetSpreadsheetByDataFilterRequest;
import com.google.api.services.sheets.v4.model.GridData;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.UpdateBordersRequest;
import com.google.api.services.sheets.v4.model.UpdateCellsRequest;
import com.google.api.services.sheets.v4.model.UpdateDimensionPropertiesRequest;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.kyc.snap.grid.Border.Style;
import com.kyc.snap.image.ImageUtils;

import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.jackson.JacksonEncoder;
import lombok.Builder;
import lombok.Data;

@Data
public class SpreadsheetManager {

    public static final int ROW_OFFSET = 1;
    public static final int COL_OFFSET = 1;

    private final GoogleCredential credential;
    private final Spreadsheets spreadsheets;
    private final String spreadsheetId;
    private final int sheetId;

    public String getUrl() {
        return getSpreadsheet().getSpreadsheetUrl();
    }

    public String getRef(int row, int col) {
        row += ROW_OFFSET;
        col += COL_OFFSET;
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

    /**
     * Returns data about the current sheet.
     */
    public SheetData getSheetData() {
        try {
            Spreadsheet spreadsheet = spreadsheets.getByDataFilter(
                spreadsheetId,
                new GetSpreadsheetByDataFilterRequest()
                    .setIncludeGridData(true))
                .execute();
            GridData gridData = findSheet(spreadsheet, sheetId).getData().get(0);

            List<Integer> rowHeights = new ArrayList<>();
            List<DimensionProperties> rowMetadata = gridData.getRowMetadata();
            for (int i = ROW_OFFSET; i < rowMetadata.size(); i++)
                rowHeights.add(rowMetadata.get(i).getPixelSize());

            List<Integer> colWidths = new ArrayList<>();
            List<DimensionProperties> colMetadata = gridData.getColumnMetadata();
            for (int i = ROW_OFFSET; i < colMetadata.size(); i++)
                colWidths.add(colMetadata.get(i).getPixelSize());

            List<List<String>> content = new ArrayList<>();
            List<RowData> rowData = gridData.getRowData();
            if (rowData != null)
                for (int i = ROW_OFFSET; i < rowData.size(); i++) {
                    List<String> contentRow = new ArrayList<>();
                    List<CellData> cellData = rowData.get(i).getValues();
                    if (cellData != null)
                        for (int j = COL_OFFSET; j < cellData.size(); j++) {
                            String contentCell = "";
                            ExtendedValue value = cellData.get(j).getEffectiveValue();
                            if (value != null) {
                                if (value.getBoolValue() != null)
                                    contentCell = value.getBoolValue().toString();
                                else if (value.getNumberValue() != null)
                                    contentCell = value.getNumberValue().toString();
                                else if (value.getStringValue() != null)
                                    contentCell = value.getStringValue();
                            }
                            contentRow.add(contentCell);
                        }
                    content.add(contentRow);
                }

            return new SheetData(rowHeights, colWidths, content);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns references for each cell, which can be used in another formula.
     */
    public Map<Point, String> getReferences(Collection<Point> coordinates) {
        try {
            List<Point> orderedCoordinates = new ArrayList<>(coordinates);
            Spreadsheet spreadsheet = spreadsheets.getByDataFilter(
                spreadsheetId,
                new GetSpreadsheetByDataFilterRequest()
                    .setIncludeGridData(true)
                    .setDataFilters(orderedCoordinates.stream()
                        .map(coordinate -> new DataFilter().setGridRange(getRange(coordinate.y, coordinate.x)))
                        .collect(Collectors.toList())))
                .execute();
            List<GridData> data = findSheet(spreadsheet, sheetId).getData();
            Map<Point, String> values = new HashMap<>();
            for (int i = 0; i < orderedCoordinates.size(); i++) {
                Point coordinate = orderedCoordinates.get(i);
                ExtendedValue value = data.get(i).getRowData().get(0).getValues().get(0).getUserEnteredValue();
                String reference = "";
                if (value != null) {
                    if (value.getBoolValue() != null)
                        reference = value.getBoolValue().toString().toUpperCase();
                    else if (value.getFormulaValue() != null) {
                        if (value.getFormulaValue().startsWith("="))
                            reference = value.getFormulaValue().substring(1);
                    } else if (value.getNumberValue() != null)
                        reference = value.getNumberValue().toString();
                    else if (value.getStringValue() != null)
                        reference = String.format("\"%s\"", value.getStringValue());
                }
                values.put(coordinate, reference);
            }
            return values;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setAllRowOrColumnSizes(Dimension dimension, int size) {
        executeRequests(
            new Request()
                .setUpdateDimensionProperties(new UpdateDimensionPropertiesRequest()
                    .setProperties(new DimensionProperties()
                        .setPixelSize(size))
                    .setFields("pixelSize")
                    .setRange(new DimensionRange()
                        .setSheetId(sheetId)
                        .setDimension(dimension.name()))));
    }

    public void setRowOrColumnSizes(Dimension dimension, List<SizedRowOrColumn> rowOrColumns) {
        executeRequests(rowOrColumns.stream()
            .map(rowOrColumn -> new Request()
                .setUpdateDimensionProperties(new UpdateDimensionPropertiesRequest()
                    .setProperties(new DimensionProperties()
                        .setPixelSize(rowOrColumn.size))
                    .setFields("pixelSize")
                    .setRange(getRange(dimension, rowOrColumn.index, rowOrColumn.index + 1))))
                    .collect(Collectors.toList()));
    }

    public void setAutomaticRowOrColumnSizes(Dimension dimension, List<Integer> rowOrColumns) {
        executeRequests(rowOrColumns.stream()
            .map(rowOrColumn -> new Request()
                .setAutoResizeDimensions(new AutoResizeDimensionsRequest()
                    .setDimensions(getRange(dimension, rowOrColumn, rowOrColumn + 1))))
            .collect(Collectors.toList()));
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

    public void setBorders(List<BorderedCell> cells) {
        executeRequests(cells.stream()
            .map(cell -> new Request()
                .setUpdateBorders(new UpdateBordersRequest()
                    .setRight(new Border()
                        .setStyle(toStyle(cell.rightBorder.getStyle()))
                        .setColor(toColor(cell.rightBorder.getRgb())))
                    .setBottom(new Border()
                        .setStyle(toStyle(cell.bottomBorder.getStyle()))
                        .setColor(toColor(cell.bottomBorder.getRgb())))
                    .setRange(getRange(cell.row, cell.col))))
            .collect(Collectors.toList()));
    }

    /**
     * Run a published Google Apps Script that adds an image to a sheet. The script is here:
     * https://script.google.com/home/projects/
     * 1pIMTaT1S2eJ2raU_fJHladPb9vrqyCyDXZybOZIxf2gAJwxoG7icMVUS
     */
    public void insertImage(BufferedImage image, int row, int col, int width, int height) {
        WebAppService webApp = Feign.builder()
                .encoder(new JacksonEncoder())
                .target(
                    WebAppService.class,
                    "https://script.google.com/macros/s/AKfycbyE4Qi9jzGMr_m8Q6T6Ddk0U-a8IfmizMyVJcO3lQ");
        try {
            String response = webApp.insertImage(getAccessToken(), InsertImageRequest.builder()
                .spreadsheetId(spreadsheetId)
                .sheetName(findSheet(getSpreadsheet(), sheetId).getProperties().getTitle())
                .url(ImageUtils.toDataURL(image))
                .column(col + 1 + COL_OFFSET)
                .row(row + 1 + ROW_OFFSET)
                .offsetX(0)
                .offsetY(0)
                .width(width)
                .height(height)
                .build());
            if (!response.contains("The script completed"))
                throw new RuntimeException(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    interface WebAppService {

        @RequestLine("POST /exec")
        @Headers("Authorization: Bearer {token}")
        String insertImage(@Param("token") String token, InsertImageRequest request);
    }

    @Data
    @Builder
    public static class InsertImageRequest {

        final String command = "insertImage";
        final String spreadsheetId;
        final String sheetName;
        final String url;
        final int column;
        final int row;
        final int offsetX;
        final int offsetY;
        final int width;
        final int height;
    }

    private void executeRequests(Request... requests) {
        executeRequests(Arrays.asList(requests));
    }

    private void executeRequests(List<Request> requests) {
        try {
            spreadsheets
                .batchUpdate(spreadsheetId, new BatchUpdateSpreadsheetRequest().setRequests(requests))
                .execute();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 403)
                throw new ForbiddenException("No permissions to edit spreadsheet");
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Spreadsheet getSpreadsheet() {
        try {
            return spreadsheets.get(spreadsheetId).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized String getAccessToken() throws IOException {
        if (credential.getAccessToken() == null) {
            credential.refreshToken();
        }
        return credential.getAccessToken();
    }

    private DimensionRange getRange(Dimension dimension, int startIndex, int endIndex) {
        return new DimensionRange()
            .setSheetId(sheetId)
            .setDimension(dimension.name())
            .setStartIndex(startIndex + (dimension == Dimension.ROWS ? ROW_OFFSET : COL_OFFSET))
            .setEndIndex(endIndex + (dimension == Dimension.ROWS ? ROW_OFFSET : COL_OFFSET));
    }

    private GridRange getRange(int row, int col) {
        return new GridRange()
            .setSheetId(sheetId)
            .setStartRowIndex(row + ROW_OFFSET)
            .setEndRowIndex(row + 1 + ROW_OFFSET)
            .setStartColumnIndex(col + COL_OFFSET)
            .setEndColumnIndex(col + 1 + COL_OFFSET);
    }

    private static Sheet findSheet(Spreadsheet spreadsheet, int sheetId) {
        return Iterables.find(spreadsheet.getSheets(), sheet -> sheet.getProperties().getSheetId() == sheetId);
    }

    private static Color toColor(int rgb) {
        java.awt.Color color = new java.awt.Color(rgb);
        return new Color()
            .setRed(color.getRed() / 255f)
            .setGreen(color.getGreen() / 255f)
            .setBlue(color.getBlue() / 255f);
    }

    private static String toStyle(Style style) {
        switch (style) {
            case NONE:
                return "NONE";
            case THIN:
                return "SOLID";
            case MEDIUM:
                return "SOLID_MEDIUM";
            case THICK:
                return "SOLID_THICK";
            default:
                throw new RuntimeException("Invalid style: " + style);
        }
    }

    @Data
    public static class SheetData {

        private final List<Integer> rowHeights;
        private final List<Integer> colWidths;
        private final List<List<String>> content;
    }

    public static enum Dimension {

        ROWS,
        COLUMNS,
    }

    @Data
    public static class SizedRowOrColumn {

        private final int index;
        private final int size;
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

    @Data
    public static class BorderedCell {

        private final int row;
        private final int col;
        private final com.kyc.snap.grid.Border rightBorder;
        private final com.kyc.snap.grid.Border bottomBorder;
    }
}
