package com.kyc.snap.server;

import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

import com.google.common.collect.ImmutableList;
import com.kyc.snap.document.Document;
import com.kyc.snap.document.Document.DocumentPage;
import com.kyc.snap.document.Document.DocumentText;
import com.kyc.snap.document.Pdf;
import com.kyc.snap.google.GoogleAPIManager;
import com.kyc.snap.google.SpreadsheetManager;
import com.kyc.snap.google.SpreadsheetManager.ValueCell;
import com.kyc.snap.image.ImageUtils;
import com.kyc.snap.store.Store;

import lombok.Data;

@Data
public class DocumentResource implements DocumentService {

    public static String MARKER = "🍊";

    private final Store store;
    private final GoogleAPIManager googleApi;

    @Override
    public Document createDocumentFromPdf(InputStream pdfStream) throws IOException {
        String id = UUID.randomUUID().toString();
        List<DocumentPage> pages = new ArrayList<>();
        try (Pdf pdf = new Pdf(IOUtils.toByteArray(pdfStream))) {
            for (int page = 0; page < pdf.getNumPages(); page++) {
                String imageId = store.storeBlob(ImageUtils.toBytes(pdf.toImage(page)));
                List<DocumentText> texts = pdf.getTexts(page);
                pages.add(new DocumentPage(imageId, texts));
            }
        }
        Document doc = new Document(id, pages);
        store.updateObject(id, doc);
        return doc;
    }

    @Override
    public Point initializeSheet(String spreadsheetId, int sheetId) {
        SpreadsheetManager spreadsheets = googleApi.getSheet(spreadsheetId, sheetId);
        List<List<String>> content = spreadsheets.getContent();
        return findMarker(content).orElseGet(() -> {
            Point marker = new Point(content.isEmpty() ? 0 : content.size() + 1, 0);
            spreadsheets.setValues(ImmutableList.of(new ValueCell(marker.x, marker.y, MARKER)));
            return marker;
        });
    }

    private static Optional<Point> findMarker(List<List<String>> content) {
        for (int i = 0; i < content.size(); i++)
            for (int j = 0; j < content.get(i).size(); j++)
                if (content.get(i).get(j).equals(MARKER))
                    return Optional.of(new Point(i, j));
        return Optional.empty();
    }
}
