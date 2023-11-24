
package com.kyc.snap.google;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
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
import com.google.api.services.sheets.v4.model.CopySheetToAnotherSpreadsheetRequest;
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
import com.google.common.collect.Iterables;
import com.kyc.snap.grid.Border.Style;
import com.kyc.snap.image.ImageUtils;
import com.kyc.snap.server.ServerProperties;

import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.jackson.JacksonEncoder;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;

public record SpreadsheetManager(
        GoogleCredential credential,
        Spreadsheets spreadsheets,
        String spreadsheetId,
        int sheetId) {

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
                .toList());
    }

    public void setAutomaticRowOrColumnSizes(Dimension dimension, List<Integer> rowOrColumns) {
        executeRequests(rowOrColumns.stream()
                .map(rowOrColumn -> new Request()
                        .setAutoResizeDimensions(new AutoResizeDimensionsRequest()
                                .setDimensions(getRange(dimension, rowOrColumn, rowOrColumn + 1))))
                .toList());
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

    public void setBackgroundColors(List<ColoredCell> cells) {
        executeRequests(cells.stream()
                .map(cell -> new Request()
                        .setUpdateCells(new UpdateCellsRequest()
                                .setRows(List.of(new RowData()
                                        .setValues(List.of(new CellData()
                                                .setUserEnteredFormat(new CellFormat()
                                                        .setBackgroundColor(toColor(cell.rgb)))))))
                                .setFields("userEnteredFormat.backgroundColor")
                                .setRange(getRange(cell.row, cell.col))))
                .toList());
    }

    public void setValues(List<ValueCell> cells) {
        executeRequests(cells.stream()
                .map(cell -> new Request()
                        .setUpdateCells(new UpdateCellsRequest()
                                .setRows(List.of(new RowData()
                                        .setValues(List.of(new CellData()
                                                .setUserEnteredValue(new ExtendedValue()
                                                        .setStringValue(cell.text))))))
                                .setFields("userEnteredValue.stringValue")
                                .setRange(getRange(cell.row, cell.col))))
                .toList());
    }

    public void setFormulas(List<ValueCell> cells) {
        executeRequests(cells.stream()
                .map(cell -> new Request()
                        .setUpdateCells(new UpdateCellsRequest()
                                .setRows(List.of(new RowData()
                                        .setValues(List.of(new CellData()
                                                .setUserEnteredValue(new ExtendedValue()
                                                        .setFormulaValue(cell.text))))))
                                .setFields("userEnteredValue.formulaValue")
                                .setRange(getRange(cell.row, cell.col))))
                .toList());
    }

    public void setBorders(List<BorderedCell> cells) {
        executeRequests(cells.stream()
                .map(cell -> new Request()
                        .setUpdateBorders(new UpdateBordersRequest()
                                .setTop(toBorder(cell.topBorder))
                                .setRight(toBorder(cell.rightBorder))
                                .setBottom(toBorder(cell.bottomBorder))
                                .setLeft(toBorder(cell.leftBorder))
                                .setRange(getRange(cell.row, cell.col))))
                .toList());
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
        String serverScriptBase = properties.googleServerScriptUrl().replaceAll("/exec$", "");
        WebAppService webApp = Feign.builder()
                .encoder(new JacksonEncoder())
                .target(WebAppService.class, serverScriptBase);

        // Grab global lock because everything uses the same draft spreadsheet
        synchronized (SpreadsheetManager.class) {
            webApp.exec(getAccessToken(), new RemoveAllImagesRequest(properties.draftSpreadsheetId(), 0));
            for (OverlayImage image : images) {
                webApp.exec(getAccessToken(), new InsertImageRequest(
                        properties.draftSpreadsheetId(),
                        0,
                        ImageUtils.toDataURL(image.image),
                        col + 1,
                        row + 1,
                        image.offsetX,
                        image.offsetY,
                        image.image.getWidth(),
                        image.image.getHeight()));
            }
            try {
                spreadsheets.sheets()
                        .copyTo(properties.draftSpreadsheetId(), 0, new CopySheetToAnotherSpreadsheetRequest()
                                .setDestinationSpreadsheetId(spreadsheetId))
                        .execute();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public record OverlayImage(BufferedImage image, int offsetX, int offsetY) {}

    interface WebAppService {

        @RequestLine("POST /exec")
        @Headers("Authorization: Bearer {token}")
        void exec(@Param("token") String token, WebAppRequest request);
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "command")
    @JsonSubTypes({
            @JsonSubTypes.Type(value = RemoveAllImagesRequest.class, name = "removeAllImages"),
            @JsonSubTypes.Type(value = InsertImageRequest.class, name = "insertImage")
    })
    interface WebAppRequest {}

    record RemoveAllImagesRequest(String spreadsheetId, int sheetId) implements WebAppRequest {}

    record InsertImageRequest(
            String spreadsheetId,
            int sheetId,
            String url,
            int column,
            int row,
            int offsetX,
            int offsetY,
            int width,
            int height) implements WebAppRequest {}

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

    private static Border toBorder(com.kyc.snap.grid.Border border) {
        return new Border()
                .setStyle(toStyle(border.style))
                .setColor(toColor(border.rgb));
    }

    private static Color toColor(int rgb) {
        java.awt.Color color = new java.awt.Color(rgb);
        return new Color()
                .setRed(color.getRed() / 255f)
                .setGreen(color.getGreen() / 255f)
                .setBlue(color.getBlue() / 255f);
    }

    private static String toStyle(Style style) {
        return switch (style) {
            case NONE -> "NONE";
            case THIN -> "SOLID";
            case MEDIUM -> "SOLID_MEDIUM";
            case THICK -> "SOLID_THICK";
        };
    }

    public record SheetData(List<Integer> rowHeights, List<Integer> colWidths, List<List<String>> content) {}

    public enum Dimension {

        ROWS,
        COLUMNS,
    }

    public record SizedRowOrColumn(int index, int size) {}

    public record ColoredCell(int row, int col, int rgb) {}

    public record ValueCell(int row, int col, String text) {}

    public record BorderedCell(
            int row,
            int col,
            com.kyc.snap.grid.Border topBorder,
            com.kyc.snap.grid.Border rightBorder,
            com.kyc.snap.grid.Border bottomBorder,
            com.kyc.snap.grid.Border leftBorder) {}
}
