package com.kyc.snap.server;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

import com.kyc.snap.document.Document;
import com.kyc.snap.document.Document.DocumentPage;
import com.kyc.snap.document.Document.DocumentText;
import com.kyc.snap.document.Pdf;
import com.kyc.snap.document.Rectangle;
import com.kyc.snap.document.Section;
import com.kyc.snap.google.GoogleAPIManager;
import com.kyc.snap.google.SpreadsheetManager;
import com.kyc.snap.google.SpreadsheetManager.SheetData;
import com.kyc.snap.google.SpreadsheetManager.ValueCell;
import com.kyc.snap.grid.Grid;
import com.kyc.snap.grid.GridLines;
import com.kyc.snap.grid.GridParser;
import com.kyc.snap.grid.GridPosition;
import com.kyc.snap.grid.GridSpreadsheetWrapper;
import com.kyc.snap.image.ImageUtils;
import com.kyc.snap.server.DocumentService.FindGridLinesRequest.FindGridLinesMode;
import com.kyc.snap.server.DocumentService.FindGridRequest.FindTextMode;
import com.kyc.snap.store.Store;

import lombok.Data;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Data
public class DocumentResource implements DocumentService {

    public static String MARKER = "🍊";

    private final Store store;
    private final GoogleAPIManager googleApi;
    private final GridParser gridParser;

    @Override
    public Document createDocumentFromPdf(InputStream pdfStream) throws IOException {
        String id = UUID.randomUUID().toString();
        List<DocumentPage> pages = new ArrayList<>();
        try (Pdf pdf = new Pdf(IOUtils.toByteArray(pdfStream))) {
            for (int page = 0; page < pdf.getNumPages(); page++) {
                String imageId = store.storeBlob(ImageUtils.toBytes(pdf.toImage(page)));
                List<DocumentText> texts = pdf.getTexts(page);
                pages.add(new DocumentPage(imageId, Pdf.RENDER_SCALE, texts));
            }
        }
        Document doc = new Document(id, pages);
        store.updateObject(id, doc);
        return doc;
    }

    @Override
    public Document createDocumentFromUrl(CreateDocumentFromUrlRequest request) throws IOException {
        Response response = new OkHttpClient()
            .newCall(new Request.Builder().url(request.getUrl()).get().build())
            .execute();
        return createDocumentFromPdf(response.body().byteStream());
    }

    @Override
    public Point initializeSheet(String spreadsheetId, int sheetId) {
        SpreadsheetManager spreadsheets = googleApi.getSheet(spreadsheetId, sheetId);
        return findOrAddMarker(spreadsheets, spreadsheets.getSheetData().getContent());
    }

    @Override
    public FindGridLinesResponse findGridLines(String documentId, FindGridLinesRequest request) {
        BufferedImage image = getSectionImage(documentId, request.getSection()).getImage();
        GridLines gridLines;
        if (request.getFindGridLinesMode() == FindGridLinesMode.EXPLICIT)
            gridLines = gridParser.findGridLines(image, 64);
        else if (request.getFindGridLinesMode() == FindGridLinesMode.IMPLICIT)
            gridLines = gridParser.findImplicitGridLines(image);
        else
            throw new RuntimeException("Invalid find grid lines mode: " + request.getFindGridLinesMode());
        GridPosition gridPosition = gridParser.getGridPosition(gridLines);
        return new FindGridLinesResponse(gridLines, gridPosition);
    }

    @Override
    public Grid findGrid(String documentId, FindGridRequest request) {
        GridPosition gridPosition = request.getGridPosition();
        Grid grid = Grid.create(gridPosition.getNumRows(), gridPosition.getNumCols());
        SectionImage image = getSectionImage(documentId, request.getSection());
        if (request.isFindColors())
            gridParser.findGridColors(image.getImage(), gridPosition, grid);
        if (request.getFindTextMode() == FindTextMode.USE_NATIVE)
            gridParser.findGridText(image.getTexts(), request.getSection().getRectangle(), gridPosition, grid);
        else if (request.getFindTextMode() == FindTextMode.USE_OCR)
            gridParser.findGridText(image.getImage(), gridPosition, grid);
        if (request.isFindBorders()) {
            gridParser.findGridBorders(image.getImage(), gridPosition, grid);
            gridParser.findGridBorderStyles(grid);
        }
        return grid;
    }

    @Override
    public Point export(String documentId, String spreadsheetId, int sheetId, ExportRequest request) {
        SectionImage image = getSectionImage(documentId, request.getSection());
        SpreadsheetManager spreadsheets = googleApi.getSheet(spreadsheetId, sheetId);
        SheetData sheetData = spreadsheets.getSheetData();
        Point marker = findOrAddMarker(spreadsheets, sheetData.getContent());
        updateMarker(Optional.of(marker), Optional.empty(), spreadsheets);
        Point newMarker;
        if (request.getGridPosition() != null && request.getGrid() != null) {
            new GridSpreadsheetWrapper(spreadsheets, marker.y, marker.x)
                .toSpreadsheet(request.getGridPosition(), request.getGrid(), 1 / image.getScale());
            newMarker = new Point(marker.x + request.getGridPosition().getNumCols() + 1, marker.y);
        } else {
            int newWidth = (int) (image.getImage().getWidth() / image.getScale());
            int newHeight = (int) (image.getImage().getHeight() / image.getScale());
            spreadsheets.insertImage(image.getImage(), marker.y, marker.x, newWidth, newHeight);
            newMarker = getNewMarker(marker, newWidth, sheetData.getColWidths());
        }
        updateMarker(Optional.empty(), Optional.of(newMarker), spreadsheets);
        return newMarker;
    }

    private SectionImage getSectionImage(String documentId, Section section) {
        Document doc = store.getObject(documentId, Document.class);
        DocumentPage page = doc.getPages().get(section.getPage());
        byte[] imageBlob = store.getBlob(page.getImageId());
        Rectangle r = section.getRectangle();
        BufferedImage image = ImageUtils.fromBytes(imageBlob)
            .getSubimage((int) r.getX(), (int) r.getY(), (int) r.getWidth(), (int) r.getHeight());
        return new SectionImage(image, page.getScale(), page.getTexts());
    }

    private static Point findOrAddMarker(SpreadsheetManager spreadsheets, List<List<String>> content) {
        for (int i = 0; i < content.size(); i++)
            for (int j = 0; j < content.get(i).size(); j++)
                if (content.get(i).get(j).equals(MARKER))
                    return new Point(j, i);
        Point marker = new Point(0, content.size() + 1);
        updateMarker(Optional.empty(), Optional.of(marker), spreadsheets);
        return marker;
    }

    private static void updateMarker(Optional<Point> marker, Optional<Point> newMarker, SpreadsheetManager spreadsheets) {
        List<ValueCell> values = new ArrayList<>();
        marker.ifPresent(m -> values.add(new ValueCell(m.y, m.x, "")));
        newMarker.ifPresent(m -> values.add(new ValueCell(m.y, m.x, MARKER)));
        spreadsheets.setValues(values);
    }

    private static Point getNewMarker(Point marker, int width, List<Integer> colWidths) {
        int col = marker.x;
        while (width > 0) {
            width -= colWidths.get(col);
            col++;
        }
        return new Point(col, marker.y);
    }

    @Data
    private static class SectionImage {

        private final BufferedImage image;
        private final double scale;
        private final List<DocumentText> texts;
    }
}
