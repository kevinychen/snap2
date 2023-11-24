package com.kyc.snap.server;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.common.io.ByteStreams;
import com.kyc.snap.api.DocumentService;
import com.kyc.snap.api.DocumentService.FindGridLinesRequest.FindGridLinesMode;
import com.kyc.snap.crossword.CrosswordClues;
import com.kyc.snap.crossword.CrosswordFormula;
import com.kyc.snap.crossword.CrosswordParser;
import com.kyc.snap.crossword.CrosswordSpreadsheetWrapper;
import com.kyc.snap.document.Document;
import com.kyc.snap.document.Document.DocumentPage;
import com.kyc.snap.document.Document.DocumentText;
import com.kyc.snap.document.Pdf;
import com.kyc.snap.document.Rectangle;
import com.kyc.snap.document.Section;
import com.kyc.snap.google.GoogleAPIManager;
import com.kyc.snap.google.PresentationManager;
import com.kyc.snap.google.PresentationManager.PositionedImage;
import com.kyc.snap.google.SpreadsheetManager;
import com.kyc.snap.google.SpreadsheetManager.OverlayImage;
import com.kyc.snap.google.SpreadsheetManager.SheetData;
import com.kyc.snap.grid.Grid;
import com.kyc.snap.grid.GridLines;
import com.kyc.snap.grid.GridParser;
import com.kyc.snap.grid.GridPosition;
import com.kyc.snap.grid.GridSpreadsheetWrapper;
import com.kyc.snap.image.ImageBlob;
import com.kyc.snap.image.ImageUtils;
import com.kyc.snap.store.Store;

import javax.imageio.ImageIO;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public record DocumentResource(
        Store store,
        GoogleAPIManager googleApi,
        GridParser gridParser,
        CrosswordParser crosswordParser) implements DocumentService {

    private static final int DOCUMENT_SIZE_LIMIT = 10_000_000;

    @Override
    public Document getDocument(String documentId) {
        return store.getObject(documentId, Document.class);
    }

    @Override
    public Document createDocumentFromPdf(InputStream pdfStream) throws IOException {
        String id = UUID.randomUUID().toString();
        List<DocumentPage> pages = new ArrayList<>();
        try (Pdf pdf = new Pdf(ByteStreams.limit(pdfStream, DOCUMENT_SIZE_LIMIT))) {
            for (int page = 0; page < pdf.getNumPages(); page++) {
                BufferedImage image = pdf.toImage(page);
                String imageId = store.storeBlob(ImageUtils.toBytes(image));
                String compressedImageId = store.storeBlob(ImageUtils.toBytesCompressed(image));
                List<DocumentText> texts = pdf.getTexts(page);
                pages.add(new DocumentPage(imageId, compressedImageId, Pdf.RENDER_SCALE, texts));
            }
        }
        Document doc = new Document(id, pages);
        store.updateObject(id, doc);
        return doc;
    }

    @Override
    public Document createDocumentFromImage(InputStream imageStream) throws IOException {
        String id = UUID.randomUUID().toString();
        BufferedImage image = ImageIO.read(ByteStreams.limit(imageStream, DOCUMENT_SIZE_LIMIT));
        String imageId = store.storeBlob(ImageUtils.toBytes(image));
        String compressedImageId = store.storeBlob(ImageUtils.toBytesCompressed(image));
        Document doc = new Document(id, List.of(new DocumentPage(imageId, compressedImageId, 1, List.of())));
        store.updateObject(id, doc);
        return doc;
    }

    @Override
    public Document createDocumentFromUrl(CreateDocumentFromUrlRequest request) throws Exception {
        String url = request.url();
        String urlExtension = url.substring(url.lastIndexOf('.') + 1).toLowerCase();
        try (Response response = new OkHttpClient().newCall(new Request.Builder().url(url).get().build()).execute()) {
            String contentType = response.header("Content-Type").toLowerCase();
            InputStream responseStream = response.body().byteStream();
            if (contentType.equals("application/pdf") || urlExtension.equals("pdf"))
                return createDocumentFromPdf(responseStream);
            else if (contentType.startsWith("image/") || urlExtension.equals("png") || urlExtension.equals("jpg"))
                return createDocumentFromImage(responseStream);
            else {
                File tempPdf = File.createTempFile("snap", ".pdf");
                try {
                    int exitCode = new ProcessBuilder("google-chrome", "--headless", "--disable-gpu", "--no-margins", "--no-sandbox",
                            "--print-to-pdf=" + tempPdf.getAbsolutePath(), url).start().waitFor();
                    if (exitCode != 0)
                        throw new IllegalStateException("Chrome printToPDF failed with exit code " + exitCode);
                    try (FileInputStream in = new FileInputStream(tempPdf)) {
                        return createDocumentFromPdf(in);
                    }
                } finally {
                    tempPdf.delete();
                }
            }
        }
    }

    @Override
    public GridLines findGridLines(String documentId, FindGridLinesRequest request) {
        BufferedImage image = getSectionImage(documentId, request.getSection()).image();
        GridLines gridLines;
        if (request.getFindGridLinesMode() == FindGridLinesMode.EXPLICIT)
            gridLines = gridParser.findGridLines(image);
        else if (request.getFindGridLinesMode() == FindGridLinesMode.IMPLICIT)
            gridLines = gridParser.findImplicitGridLines(image);
        else
            throw new RuntimeException("Invalid find grid lines mode: " + request.getFindGridLinesMode());
        if (request.isInterpolate())
            gridLines = gridParser.getInterpolatedGridLines(gridLines);
        return gridLines;
    }

    @Override
    public List<ImageBlob> findBlobs(String documentId, FindBlobsRequest request) {
        BufferedImage image = getSectionImage(documentId, request.getSection()).image();
        return ImageUtils.findBlobs(image, request.isExact()).stream()
                .filter(blob -> blob.width() >= request.getMinBlobSize() && blob.height() >= request.getMinBlobSize())
                .toList();
    }

    @Override
    public FindGridResponse findGrid(String documentId, FindGridRequest request) {
        GridLines gridLines = request.getGridLines();
        GridPosition gridPosition = gridParser.getGridPosition(gridLines);
        Grid grid = Grid.create(gridPosition.getNumRows(), gridPosition.getNumCols());
        SectionImage image = getSectionImage(documentId, request.getSection());
        gridParser.findGridColors(image.image(), gridPosition, grid);
        gridParser.findGridBorders(image.image(), gridPosition, grid);
        gridParser.findGridBorderStyles(grid);
        gridParser.findGridText(image.texts(), request.getSection().rectangle(), gridPosition, grid);
        return new FindGridResponse(gridPosition, grid);
    }

    @Override
    public FindCrosswordCluesResponse findCrosswordClues(String documentId, FindCrosswordCluesRequest request) {
        CrosswordClues clues = crosswordParser.parseClues(getDocument(documentId), request.crossword());
        List<CrosswordFormula> formulas = crosswordParser.getFormulas(request.crossword(), clues);
        return new FindCrosswordCluesResponse(clues, formulas);
    }

    @Override
    public boolean exportSheet(String documentId, String spreadsheetId, int sheetId, ExportSheetRequest request) {
        SectionImage image = getSectionImage(documentId, request.section());
        SpreadsheetManager spreadsheets = googleApi.getSheet(spreadsheetId, sheetId);
        SheetData sheetData = spreadsheets.getSheetData();
        Marker marker = request.marker();
        GridPosition gridPosition = request.gridLines() == null ? null : gridParser.getGridPosition(request.gridLines());
        if (gridPosition != null && request.grid() != null) {
            new GridSpreadsheetWrapper(spreadsheets, marker.row(), marker.col())
                    .toSpreadsheet(gridPosition, request.grid(), sheetData);
            if (request.crossword() != null && request.crosswordClues() != null) {
                new CrosswordSpreadsheetWrapper(spreadsheets, marker.row(), marker.col())
                        .toSpreadsheet(request.grid(), request.crossword(), request.crosswordClues());
            }
        } else if (request.blobs() != null) {
            spreadsheets.createNewSheetWithImages(marker.row(), marker.col(), request.blobs().stream()
                    .map(blob -> {
                        BufferedImage blobImage = ImageUtils.getBlobImage(image.image(), blob);
                        BufferedImage scaledImage = ImageUtils.scale(blobImage, 1. / image.scale());
                        int offsetX = (int) (blob.x() / image.scale());
                        int offsetY = (int) (blob.y() / image.scale());
                        return new OverlayImage(scaledImage, offsetX, offsetY);
                    })
                    .toList());
        }
        return true;
    }

    @Override
    public boolean exportSlide(String documentId, String presentationId, String slideId, ExportSlideRequest request) {
        SectionImage image = getSectionImage(documentId, request.section());
        PresentationManager presentations = googleApi.getPresentation(presentationId, slideId);
        if (request.blobs() != null) {
            presentations.addImages(
                    request.blobs().stream()
                            .map(blob -> new PositionedImage(ImageUtils.getBlobImage(image.image, blob), blob.x(), blob.y()))
                            .toList(),
                    image.image.getWidth(),
                    image.image.getHeight());
        }
        return true;
    }

    private SectionImage getSectionImage(String documentId, Section section) {
        Document doc = getDocument(documentId);
        DocumentPage page = doc.pages().get(section.page());
        byte[] imageBlob = store.getBlob(page.imageId());
        Rectangle r = section.rectangle();
        BufferedImage image = ImageUtils.fromBytes(imageBlob)
                .getSubimage((int) r.x(), (int) r.y(), (int) r.width(), (int) r.height());
        return new SectionImage(image, page.scale(), page.texts());
    }

    private record SectionImage(BufferedImage image, double scale, List<DocumentText> texts) {}
}
