
package com.kyc.snap.google;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets;
import com.google.api.services.sheets.v4.model.AutoResizeDimensionsRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.Border;
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
import com.google.api.services.sheets.v4.model.UpdateBordersRequest;
import com.google.api.services.sheets.v4.model.UpdateCellsRequest;
import com.google.api.services.sheets.v4.model.UpdateDimensionPropertiesRequest;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.kyc.snap.grid.Border.Style;
import com.kyc.snap.image.ImageUtils;

import lombok.Data;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;

@Data
public class SpreadsheetManager {

    public static final int ROW_OFFSET = 1;
    public static final int COL_OFFSET = 1;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final GoogleCredential credential;
    private final Spreadsheets spreadsheets;
    private final Spreadsheet spreadsheet;
    private final int sheetId;

    public String getUrl() {
        return spreadsheet.getSpreadsheetUrl();
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
                    .setRange(getColumnRange(startIndex, endIndex))));
    }

    public void setAutomaticColumnWidths(int startIndex, int endIndex) {
        executeRequests(
            new Request()
                .setAutoResizeDimensions(new AutoResizeDimensionsRequest()
                    .setDimensions(getColumnRange(startIndex, endIndex))));
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
     * Run a published Google App Script that adds an image to a sheet.
     *
     * The published script looks like the following:
     *
     * <pre>
     * function doPost(e) {
     *   var params = JSON.parse(e.postData.contents);
     *
     *   if (SpreadsheetApp.openById(params.spreadsheetId).getOwner().getEmail() != Session.getActiveUser().getEmail()) {
     *     console.error("Incorrect user");
     *     return;
     *   }
     *
     *   SpreadsheetApp.openById(params.spreadsheetId).getSheetByName(params.sheetName).insertImage(params.url, params.col, params.row);
     * }
     * </pre>
     *
     * https://stackoverflow.com/questions/43664483/insert-image-into-google-sheets-cell-using-
     * google-sheets-api
     */
    public void addImage(BufferedImage image, int row, int col) {
        try {
            Map<?, ?> body = ImmutableMap.of(
                "spreadsheetId", spreadsheet.getSpreadsheetId(),
                "sheetName", getSheetName(),
                "url", ImageUtils.toDataURL(image),
                "row", row + 1 + ROW_OFFSET,
                "col", col + 1 + COL_OFFSET);
            Response response = new OkHttpClient()
                .newCall(new okhttp3.Request.Builder()
                    .url("https://script.google.com/macros/s/AKfycbwtDXpf8019jeigSig5AnEc4QW-rsU_K3NTpfz5vUE0c-ZwT1NV/exec")
                    .addHeader("Authorization", "Bearer " + getAccessToken())
                    .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), MAPPER.writeValueAsBytes(body)))
                    .build())
                .execute();
            String responseMessage = response.body().string();
            if (!responseMessage.contains("The script completed"))
                throw new RuntimeException(responseMessage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    private String getSheetName() {
        return Iterables.find(spreadsheet.getSheets(), sheet -> sheet.getProperties().getSheetId() == sheetId)
            .getProperties()
            .getTitle();
    }

    private synchronized String getAccessToken() throws IOException {
        if (credential.getAccessToken() == null) {
            credential.refreshToken();
        }
        return credential.getAccessToken();
    }

    private DimensionRange getColumnRange(int startIndex, int endIndex) {
        return new DimensionRange()
            .setSheetId(sheetId)
            .setDimension("COLUMNS")
            .setStartIndex(startIndex + COL_OFFSET)
            .setEndIndex(endIndex + COL_OFFSET);
    }

    private GridRange getRange(int row, int col) {
        return new GridRange()
            .setSheetId(sheetId)
            .setStartRowIndex(row + ROW_OFFSET)
            .setEndRowIndex(row + 1 + ROW_OFFSET)
            .setStartColumnIndex(col + COL_OFFSET)
            .setEndColumnIndex(col + 1 + COL_OFFSET);
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
