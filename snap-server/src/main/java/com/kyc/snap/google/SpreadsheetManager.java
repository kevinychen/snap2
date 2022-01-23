
package com.kyc.snap.google;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.api.services.sheets.v4.model.CopySheetToAnotherSpreadsheetRequest;
import com.kyc.snap.server.ServerProperties;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets;
import com.google.api.services.sheets.v4.model.AddProtectedRangeRequest;
import com.google.api.services.sheets.v4.model.AutoResizeDimensionsRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.Border;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.Color;
import com.google.api.services.sheets.v4.model.DeleteProtectedRangeRequest;
import com.google.api.services.sheets.v4.model.DimensionProperties;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.api.services.sheets.v4.model.GetSpreadsheetByDataFilterRequest;
import com.google.api.services.sheets.v4.model.GridData;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.InsertDimensionRequest;
import com.google.api.services.sheets.v4.model.ProtectedRange;
import com.google.api.services.sheets.v4.model.RepeatCellRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.TextFormat;
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

    private final GoogleCredential credential;
    private final Spreadsheets spreadsheets;
    private final String spreadsheetId;
    private final int sheetId;

    public String getUrl() {
        return getSpreadsheet().getSpreadsheetUrl();
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
            for (DimensionProperties row : rowMetadata)
                rowHeights.add(row.getPixelSize());

            List<Integer> colWidths = new ArrayList<>();
            List<DimensionProperties> colMetadata = gridData.getColumnMetadata();
            for (DimensionProperties col : colMetadata)
                colWidths.add(col.getPixelSize());

            List<List<String>> content = new ArrayList<>();
            List<RowData> rowData = gridData.getRowData();
            if (rowData != null)
                for (RowData row : rowData) {
                    List<String> contentRow = new ArrayList<>();
                    List<CellData> cellData = row.getValues();
                    if (cellData != null)
                        for (CellData cell : cellData) {
                            String contentCell = "";
                            ExtendedValue value = cell.getEffectiveValue();
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
        } catch (GoogleJsonResponseException e) {
            throw toClientErrorException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void insertRowOrColumns(Dimension dimension, int index, int numRowOrColumns) {
        executeRequests(
            new Request()
                .setInsertDimension(new InsertDimensionRequest()
                    .setInheritFromBefore(true)
                    .setRange(new DimensionRange()
                        .setSheetId(sheetId)
                        .setDimension(dimension.name())
                        .setStartIndex(index)
                        .setEndIndex(index + numRowOrColumns))));
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

    public void setProtectedRange(int rowIndex, int numRows, int colIndex, int numCols) {
        executeRequests(new Request()
            .setAddProtectedRange(new AddProtectedRangeRequest()
                .setProtectedRange(new ProtectedRange()
                    .setRange(new GridRange()
                        .setSheetId(sheetId)
                        .setStartRowIndex(rowIndex)
                        .setEndRowIndex(rowIndex + numRows)
                        .setStartColumnIndex(colIndex)
                        .setEndColumnIndex(colIndex + numCols))
                    .setWarningOnly(true))));
    }

    public void clearProtectedRanges() {
        try {
            Spreadsheet spreadsheet = spreadsheets.get(spreadsheetId).execute();
            executeRequests(findSheet(spreadsheet, sheetId).getProtectedRanges().stream()
                .map(protectedRange -> new Request()
                    .setDeleteProtectedRange(new DeleteProtectedRangeRequest()
                        .setProtectedRangeId(protectedRange.getProtectedRangeId())))
                .collect(Collectors.toList()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
                    .setTop(new Border()
                        .setStyle(toStyle(cell.topBorder.getStyle()))
                        .setColor(toColor(cell.topBorder.getRgb())))
                    .setRight(new Border()
                        .setStyle(toStyle(cell.rightBorder.getStyle()))
                        .setColor(toColor(cell.rightBorder.getRgb())))
                    .setBottom(new Border()
                        .setStyle(toStyle(cell.bottomBorder.getStyle()))
                        .setColor(toColor(cell.bottomBorder.getRgb())))
                    .setLeft(new Border()
                        .setStyle(toStyle(cell.leftBorder.getStyle()))
                        .setColor(toColor(cell.leftBorder.getRgb())))
                    .setRange(getRange(cell.row, cell.col))))
            .collect(Collectors.toList()));
    }

    public void setTextAlignment(int rowIndex, int numRows, int colIndex, int numCols, String horizontalAlignment, String verticalAlignment) {
        executeRequests(new Request()
            .setRepeatCell(new RepeatCellRequest()
                .setRange(new GridRange()
                    .setSheetId(sheetId)
                    .setStartRowIndex(rowIndex)
                    .setEndRowIndex(rowIndex + numRows)
                    .setStartColumnIndex(colIndex)
                    .setEndColumnIndex(colIndex + numCols))
                .setCell(new CellData()
                    .setUserEnteredFormat(new CellFormat()
                        .setHorizontalAlignment(horizontalAlignment)
                        .setVerticalAlignment(verticalAlignment)))
                .setFields("userEnteredFormat(horizontalAlignment,verticalAlignment)")));
    }

    public void setFont(int rowIndex, int numRows, int colIndex, int numCols, String fontFamily) {
        executeRequests(new Request()
            .setRepeatCell(new RepeatCellRequest()
                .setRange(new GridRange()
                    .setSheetId(sheetId)
                    .setStartRowIndex(rowIndex)
                    .setEndRowIndex(rowIndex + numRows)
                    .setStartColumnIndex(colIndex)
                    .setEndColumnIndex(colIndex + numCols))
                .setCell(new CellData().setUserEnteredFormat(new CellFormat()
                    .setTextFormat(new TextFormat().setFontFamily(fontFamily))))
                .setFields("userEnteredFormat.textFormat.fontFamily")));
    }

    public void createNewSheetWithImages(int row, int col, List<OverlayImage> images) {
        ServerProperties properties = ServerProperties.get();

        // Passing the full URL here with an empty path in the Feign interface declaration doesn't work because there's
        // an extra ending slash, so we clip off the ending "/exec" and declare it in the Feign interface instead.
        String serverScriptBase = properties.getGoogleServerScriptUrl().replaceAll("/exec$", "");
        WebAppService webApp = Feign.builder()
            .encoder(new JacksonEncoder())
            .target(WebAppService.class, serverScriptBase);

        // Grab global lock because everything uses the same draft spreadsheet
        synchronized (SpreadsheetManager.class) {
            webApp.removeAllImages(getAccessToken(), RemoveAllImagesRequest.builder()
                .spreadsheetId(properties.getDraftSpreadsheetId())
                .sheetId(0)
                .build());
            images.stream()
                // images may be large, so add in parallel
                .parallel()
                .forEach(image -> webApp.insertImage(getAccessToken(), InsertImageRequest.builder()
                    .spreadsheetId(properties.getDraftSpreadsheetId())
                    .sheetId(0)
                    .url(ImageUtils.toDataURL(image.image))
                    .column(col + 1)
                    .row(row + 1)
                    .offsetX(image.offsetX)
                    .offsetY(image.offsetY)
                    .width(image.image.getWidth())
                    .height(image.image.getHeight())
                    .build()));
            try {
                spreadsheets.sheets()
                    .copyTo(properties.getDraftSpreadsheetId(), 0, new CopySheetToAnotherSpreadsheetRequest()
                        .setDestinationSpreadsheetId(spreadsheetId))
                    .execute();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Data
    public static class OverlayImage {

        final BufferedImage image;
        final int offsetX;
        final int offsetY;
    }

    interface WebAppService {

        @RequestLine("POST /exec")
        @Headers("Authorization: Bearer {token}")
        String removeAllImages(@Param("token") String token, RemoveAllImagesRequest request);

        @RequestLine("POST /exec")
        @Headers("Authorization: Bearer {token}")
        String insertImage(@Param("token") String token, InsertImageRequest request);
    }

    @Data
    @Builder
    public static class RemoveAllImagesRequest {

        final String command = "removeAllImages";
        final String spreadsheetId;
        final int sheetId;
    }

    @Data
    @Builder
    public static class InsertImageRequest {

        final String command = "insertImage";
        final String spreadsheetId;
        final int sheetId;
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
            throw toClientErrorException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ClientErrorException toClientErrorException(GoogleJsonResponseException e) {
        if (e.getStatusCode() == 403) {
            return new ForbiddenException(
                String.format("Insufficient permissions. "
                        + "Please grant '%s' edit permissions to your Google sheet or a parent folder and try again.",
                    credential.getServiceAccountId()),
                e);
        }
        return new ClientErrorException(Response.Status.fromStatusCode(e.getStatusCode()), e);
    }

    private Spreadsheet getSpreadsheet() {
        try {
            return spreadsheets.get(spreadsheetId).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized String getAccessToken() {
        try {
            if (credential.getAccessToken() == null) {
                credential.refreshToken();
            }
            return credential.getAccessToken();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private DimensionRange getRange(Dimension dimension, int startIndex, int endIndex) {
        return new DimensionRange()
            .setSheetId(sheetId)
            .setDimension(dimension.name())
            .setStartIndex(startIndex)
            .setEndIndex(endIndex);
    }

    private GridRange getRange(int row, int col) {
        return new GridRange()
            .setSheetId(sheetId)
            .setStartRowIndex(row)
            .setEndRowIndex(row + 1)
            .setStartColumnIndex(col)
            .setEndColumnIndex(col + 1);
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

    public enum Dimension {

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
        private final com.kyc.snap.grid.Border topBorder;
        private final com.kyc.snap.grid.Border rightBorder;
        private final com.kyc.snap.grid.Border bottomBorder;
        private final com.kyc.snap.grid.Border leftBorder;
    }
}
