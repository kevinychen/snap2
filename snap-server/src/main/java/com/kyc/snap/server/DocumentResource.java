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
import com.kyc.snap.google.GoogleAPIManager;
import com.kyc.snap.google.SpreadsheetManager;
import com.kyc.snap.google.SpreadsheetManager.SheetData;
import com.kyc.snap.google.SpreadsheetManager.ValueCell;
import com.kyc.snap.grid.GridLines;
import com.kyc.snap.grid.GridParser;
import com.kyc.snap.image.ImageUtils;
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
    public Point exportSection(String documentId, String spreadsheetId, int sheetId, Section section) {
        SectionImage image = getSectionImage(documentId, section);
        SpreadsheetManager spreadsheets = googleApi.getSheet(spreadsheetId, sheetId);
        SheetData sheetData = spreadsheets.getSheetData();
        Point marker = findOrAddMarker(spreadsheets, sheetData.getContent());
        int newWidth = (int) (image.getImage().getWidth() / image.getScale());
        int newHeight = (int) (image.getImage().getHeight() / image.getScale());
        spreadsheets.insertImage(image.getImage(), marker.y, marker.x, newWidth, newHeight);
        Point newMarker = getNewMarker(marker, newWidth, sheetData.getColWidths());
        updateMarker(Optional.of(marker), Optional.of(newMarker), spreadsheets);
        return newMarker;
    }

    @Override
    public GridLines findGridLines(String documentId, Section section) {
        SectionImage image = getSectionImage(documentId, section);
        return gridParser.findGridLines(image.getImage(), 32);
    }

    private SectionImage getSectionImage(String documentId, Section section) {
        Document doc = store.getObject(documentId, Document.class);
        DocumentPage page = doc.getPages().get(section.getPage());
        byte[] imageBlob = store.getBlob(page.getImageId());
        Rectangle r = section.getRectangle();
        BufferedImage image = ImageUtils.fromBytes(imageBlob)
            .getSubimage((int) r.getX(), (int) r.getY(), (int) r.getWidth(), (int) r.getHeight());
        return new SectionImage(image, page.getScale());
    }

    private static Point findOrAddMarker(SpreadsheetManager spreadsheets, List<List<String>> content) {
        for (int i = 0; i < content.size(); i++)
            for (int j = 0; j < content.get(i).size(); j++)
                if (content.get(i).get(j).equals(MARKER))
                    return new Point(j, i);
        Point marker = new Point(0, content.isEmpty() ? 0 : content.size() + 1);
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
    }
}
