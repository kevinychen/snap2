package com.kyc.snap.server;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
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
import lombok.Data;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Data
public class DocumentResource implements DocumentService {

    private static final int DOCUMENT_SIZE_LIMIT = 10_000_000;

    private final Store store;
    private final GoogleAPIManager googleApi;
    private final GridParser gridParser;
    private final CrosswordParser crosswordParser;

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
        Document doc = new Document(id, ImmutableList.of(new DocumentPage(imageId, compressedImageId, 1, ImmutableList.of())));
        store.updateObject(id, doc);
        return doc;
    }

    @Override
    public Document createDocumentFromUrl(CreateDocumentFromUrlRequest request) throws Exception {
        String url = request.getUrl();
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
        BufferedImage image = getSectionImage(documentId, request.getSection()).getImage();
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
        BufferedImage image = getSectionImage(documentId, request.getSection()).getImage();
        return ImageUtils.findBlobs(image, request.isExact()).stream()
            .filter(blob -> blob.getWidth() >= request.getMinBlobSize() && blob.getHeight() >= request.getMinBlobSize())
            .collect(Collectors.toList());
    }

    @Override
    public FindGridResponse findGrid(String documentId, FindGridRequest request) {
        GridLines gridLines = request.getGridLines();
        GridPosition gridPosition = gridParser.getGridPosition(gridLines);
        Grid grid = Grid.create(gridPosition.getNumRows(), gridPosition.getNumCols());
        SectionImage image = getSectionImage(documentId, request.getSection());
        gridParser.findGridColors(image.getImage(), gridPosition, grid);
        gridParser.findGridBorders(image.getImage(), gridPosition, grid);
        gridParser.findGridBorderStyles(grid);
        gridParser.findGridText(image.getTexts(), request.getSection().getRectangle(), gridPosition, grid);
        return new FindGridResponse(gridPosition, grid);
    }

    @Override
    public FindCrosswordCluesResponse findCrosswordClues(String documentId, FindCrosswordCluesRequest request) {
        CrosswordClues clues = crosswordParser.parseClues(getDocument(documentId), request.getCrossword());
        List<CrosswordFormula> formulas = crosswordParser.getFormulas(request.getCrossword(), clues);
        return new FindCrosswordCluesResponse(clues, formulas);
    }

    @Override
    public boolean exportSheet(String documentId, String spreadsheetId, int sheetId, ExportSheetRequest request) {
        SectionImage image = getSectionImage(documentId, request.getSection());
        SpreadsheetManager spreadsheets = googleApi.getSheet(spreadsheetId, sheetId);
        SheetData sheetData = spreadsheets.getSheetData();
        Marker marker = request.getMarker();
        GridPosition gridPosition = request.getGridLines() == null ? null : gridParser.getGridPosition(request.getGridLines());
        if (gridPosition != null && request.getGrid() != null) {
            new GridSpreadsheetWrapper(spreadsheets, marker.getRow(), marker.getCol())
                .toSpreadsheet(gridPosition, request.getGrid(), sheetData);
            if (request.getCrossword() != null && request.getCrosswordClues() != null) {
                new CrosswordSpreadsheetWrapper(spreadsheets, marker.getRow(), marker.getCol())
                    .toSpreadsheet(request.getGrid(), request.getCrossword(), request.getCrosswordClues());
            }
        } else if (request.getBlobs() != null) {
            spreadsheets.createNewSheetWithImages(marker.getRow(), marker.getCol(), request.getBlobs().stream()
                .map(blob -> {
                    BufferedImage blobImage = ImageUtils.getBlobImage(image.getImage(), blob);
                    BufferedImage scaledImage = ImageUtils.scale(blobImage, 1. / image.getScale());
                    int offsetX = (int) (blob.getX() / image.getScale());
                    int offsetY = (int) (blob.getY() / image.getScale());
                    return new OverlayImage(scaledImage, offsetX, offsetY);
                })
                .collect(Collectors.toList()));
        }
        return true;
    }

    @Override
    public boolean exportSlide(String documentId, String presentationId, String slideId, ExportSlideRequest request) {
        SectionImage image = getSectionImage(documentId, request.getSection());
        PresentationManager presentations = googleApi.getPresentation(presentationId, slideId);
        if (request.getBlobs() != null) {
            presentations.addImages(
                request.getBlobs().stream()
                    .map(blob -> new PositionedImage(ImageUtils.getBlobImage(image.image, blob), blob.getX(), blob.getY()))
                    .collect(Collectors.toList()),
                image.image.getWidth(),
                image.image.getHeight());
        }
        return true;
    }

    private SectionImage getSectionImage(String documentId, Section section) {
        Document doc = getDocument(documentId);
        DocumentPage page = doc.getPages().get(section.getPage());
        byte[] imageBlob = store.getBlob(page.getImageId());
        Rectangle r = section.getRectangle();
        BufferedImage image = ImageUtils.fromBytes(imageBlob)
            .getSubimage((int) r.getX(), (int) r.getY(), (int) r.getWidth(), (int) r.getHeight());
        return new SectionImage(image, page.getScale(), page.getTexts());
    }

    @Data
    private static class SectionImage {

        private final BufferedImage image;
        private final double scale;
        private final List<DocumentText> texts;
    }
}
