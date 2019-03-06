package com.kyc.snap.server;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;

import com.google.common.collect.ImmutableList;
import com.kyc.snap.api.DocumentService;
import com.kyc.snap.api.DocumentService.FindGridLinesRequest.FindGridLinesMode;
import com.kyc.snap.api.DocumentService.FindGridRequest.FindTextMode;
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
import com.kyc.snap.google.SpreadsheetManager.SheetData;
import com.kyc.snap.google.SpreadsheetManager.ValueCell;
import com.kyc.snap.grid.Grid;
import com.kyc.snap.grid.GridLines;
import com.kyc.snap.grid.GridParser;
import com.kyc.snap.grid.GridPosition;
import com.kyc.snap.grid.GridSpreadsheetWrapper;
import com.kyc.snap.image.ImageBlob;
import com.kyc.snap.image.ImageUtils;
import com.kyc.snap.store.Store;
import com.kyc.snap.util.Utils;

import lombok.Data;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Data
public class DocumentResource implements DocumentService {

    public static String MARKER = "🍊";
    public static String DEFAULT_FOLDER_ID = "1kmRcpAj_O3UYZgnlkclVBHPseB2Swkd-";
    public static int FIND_BLOBS_BORDER_WIDTH = 10;

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
    public Document createDocumentFromImage(InputStream imageStream) throws IOException {
        String id = UUID.randomUUID().toString();
        String imageId = store.storeBlob(ImageUtils.toBytes(ImageIO.read(imageStream)));
        Document doc = new Document(id, ImmutableList.of(new DocumentPage(imageId, 1, ImmutableList.of())));
        store.updateObject(id, doc);
        return doc;
    }

    @Override
    public Document createDocumentFromUrl(CreateDocumentFromUrlRequest request) throws IOException {
        String url = request.getUrl();
        String urlExtension = url.substring(url.lastIndexOf('.') + 1).toLowerCase();
        Response response = new OkHttpClient()
            .newCall(new Request.Builder().url(url).get().build())
            .execute();
        String contentType = response.header("Content-Type").toLowerCase();
        InputStream responseStream = response.body().byteStream();
        if (contentType.equals("application/pdf") || urlExtension.equals("pdf"))
            return createDocumentFromPdf(responseStream);
        else if (contentType.startsWith("image/") || urlExtension.equals("png") || urlExtension.equals("jpg"))
            return createDocumentFromImage(responseStream);
        else
            throw new IllegalArgumentException("Invalid URL");
    }

    @Override
    public GridLines findGridLines(String documentId, FindGridLinesRequest request) {
        BufferedImage image = getSectionImage(documentId, request.getSection()).getImage();
        GridLines gridLines;
        if (request.getFindGridLinesMode() == FindGridLinesMode.EXPLICIT)
            gridLines = gridParser.findGridLines(image, request.getApproxGridSquareSize());
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
        List<Integer> borderRgbs = new ArrayList<>();
        for (int x = 0; x < image.getWidth(); x++)
            for (int y = 0; y < image.getHeight(); y++)
                if (x < FIND_BLOBS_BORDER_WIDTH || x >= image.getWidth() - FIND_BLOBS_BORDER_WIDTH
                || y < FIND_BLOBS_BORDER_WIDTH || y >= image.getHeight() - FIND_BLOBS_BORDER_WIDTH) {
                    borderRgbs.add(image.getRGB(x, y));
                }
        int borderRgb = Utils.mode(borderRgbs);
        return ImageUtils.findBlobs(image, rgb -> rgb != borderRgb).stream()
            .filter(blob -> blob.getX() > 1 && blob.getX() + blob.getWidth() < image.getWidth() - 1
                    && blob.getY() > 1 && blob.getY() + blob.getHeight() < image.getHeight() - 1
                    && blob.getWidth() >= request.getMinBlobSize() && blob.getHeight() >= request.getMinBlobSize())
            .collect(Collectors.toList());
    }

    @Override
    public FindGridResponse findGrid(String documentId, FindGridRequest request) {
        GridLines gridLines = request.getGridLines();
        GridPosition gridPosition = gridParser.getGridPosition(gridLines);
        Grid grid = Grid.create(gridPosition.getNumRows(), gridPosition.getNumCols());
        SectionImage image = getSectionImage(documentId, request.getSection());
        if (request.isFindColors())
            gridParser.findGridColors(image.getImage(), gridPosition, grid);
        if (request.getFindTextMode() == FindTextMode.USE_NATIVE)
            gridParser.findGridText(image.getTexts(), request.getSection().getRectangle(), gridPosition, grid);
        else if (request.getFindTextMode() == FindTextMode.USE_OCR)
            gridParser.findGridText(image.getImage(), gridPosition, grid, request.getOcrOptions());
        if (request.isFindBorders()) {
            gridParser.findGridBorders(image.getImage(), gridPosition, grid);
            gridParser.findGridBorderStyles(grid);
        }
        return new FindGridResponse(gridPosition, grid);
    }

    @Override
    public boolean exportSheet(String documentId, String spreadsheetId, int sheetId, ExportSheetRequest request) {
        SectionImage image = getSectionImage(documentId, request.getSection());
        SpreadsheetManager spreadsheets = googleApi.getSheet(spreadsheetId, sheetId);
        SheetData sheetData = spreadsheets.getSheetData();
        Point marker = findMarker(spreadsheets, sheetData.getContent());
        if (request.getGridPosition() != null && request.getGrid() != null) {
            new GridSpreadsheetWrapper(spreadsheets, marker.y, marker.x)
                .toSpreadsheet(request.getGridPosition(), request.getGrid(), image.getImage());
            if (request.getCrossword() != null && request.getCrosswordClues() != null) {
                new CrosswordSpreadsheetWrapper(spreadsheets, marker.y, marker.x)
                    .toSpreadsheet(request.getGrid(), request.getCrossword(), request.getCrosswordClues());
            }
        } else if (request.getBlobs() != null) {
            for (ImageBlob blob : request.getBlobs()) {
                BufferedImage blobImage = ImageUtils.getBlobImage(image.getImage(), blob);
                spreadsheets.insertImage(
                    blobImage,
                    marker.y,
                    marker.x,
                    (int) (blob.getWidth() / image.getScale()),
                    (int) (blob.getHeight() / image.getScale()),
                    blob.getX(),
                    blob.getY());
            }
        } else {
            int newWidth = (int) (image.getImage().getWidth() / image.getScale());
            int newHeight = (int) (image.getImage().getHeight() / image.getScale());
            spreadsheets.insertImage(image.getImage(), marker.y, marker.x, newWidth, newHeight, 0, 0);
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
        } else {
            presentations.addImages(
                ImmutableList.of(new PositionedImage(image.image, 0, 0)),
                image.image.getWidth(),
                image.image.getHeight());
        }
        return true;
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

    private static Point findMarker(SpreadsheetManager spreadsheets, List<List<String>> content) {
        for (int i = 0; i < content.size(); i++)
            for (int j = 0; j < content.get(i).size(); j++)
                if (content.get(i).get(j).equals(MARKER)) {
                    spreadsheets.setValues(ImmutableList.of(new ValueCell(i, j, "")));
                    return new Point(j, i);
                }
        return new Point(0, content.size() + 1);
    }

    @Data
    private static class SectionImage {

        private final BufferedImage image;
        private final double scale;
        private final List<DocumentText> texts;
    }
}
