package com.kyc.snap.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.FormDataParam;

import com.kyc.snap.crossword.Crossword;
import com.kyc.snap.crossword.CrosswordClues;
import com.kyc.snap.document.Document;
import com.kyc.snap.document.Section;
import com.kyc.snap.grid.Grid;
import com.kyc.snap.grid.GridLines;
import com.kyc.snap.grid.GridPosition;
import com.kyc.snap.image.ImageBlob;
import com.kyc.snap.opencv.OpenCvManager.OcrOptions;

import lombok.Data;

@Path("/documents")
public interface DocumentService {

    @POST
    @Path("/pdf")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    Document createDocumentFromPdf(@FormDataParam("pdf") InputStream pdfStream) throws IOException;

    @POST
    @Path("/image")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    Document createDocumentFromImage(@FormDataParam("image") InputStream imageStream) throws IOException;

    @POST
    @Path("/url")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Document createDocumentFromUrl(CreateDocumentFromUrlRequest request) throws IOException;

    @Data
    public static class CreateDocumentFromUrlRequest {

        private final String url;
    }

    @Data
    public static class CreateSheetResponse {

        private final String spreadsheetId;
        private final int sheetId;
    }

    @POST
    @Path("/{documentId}/lines")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    GridLines findGridLines(@PathParam("documentId") String documentId, FindGridLinesRequest request);

    @Data
    public static class FindGridLinesRequest {

        private final Section section;
        private FindGridLinesMode findGridLinesMode = FindGridLinesMode.EXPLICIT;
        private boolean interpolate = false;
        private int approxGridSquareSize = 32;

        public enum FindGridLinesMode {

            EXPLICIT,
            IMPLICIT,
        }
    }

    @POST
    @Path("/{documentId}/blobs")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    List<ImageBlob> findBlobs(@PathParam("documentId") String documentId, FindBlobsRequest request);

    @Data
    public static class FindBlobsRequest {

        private final Section section;
        private int minBlobSize = 6;
    }

    @POST
    @Path("/{documentId}/grid")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    FindGridResponse findGrid(@PathParam("documentId") String documentId, FindGridRequest request);

    @Data
    public static class FindGridRequest {

        private final Section section;
        private final GridLines gridLines;
        private boolean findColors = false;
        private boolean findBorders = false;
        private FindTextMode findTextMode = FindTextMode.NONE;
        private OcrOptions ocrOptions = new OcrOptions();

        public enum FindTextMode {

            NONE,
            USE_NATIVE,
            USE_OCR,
        }
    }

    @Data
    public static class FindGridResponse {

        private final GridPosition gridPosition;
        private final Grid grid;
    }

    @POST
    @Path("/{documentId}/export/sheet/{spreadsheetId}/{sheetId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    boolean exportSheet(@PathParam("documentId") String documentId, @PathParam("spreadsheetId") String spreadsheetId,
            @PathParam("sheetId") int sheetId, ExportSheetRequest request);

    @Data
    public static class ExportSheetRequest {

        private Marker marker = null;

        private final Section section;
        private GridPosition gridPosition;
        private Grid grid;
        private Crossword crossword;
        private CrosswordClues crosswordClues;
        private List<ImageBlob> blobs;
    }

    @Data
    public static class Marker {

        private final int row;
        private final int col;
    }

    @POST
    @Path("/{documentId}/export/slide/{presentationId}/{slideId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    boolean exportSlide(@PathParam("documentId") String documentId, @PathParam("presentationId") String presentationId,
            @PathParam("slideId") String slideId, ExportSlideRequest request);

    @Data
    public static class ExportSlideRequest {

        private final Section section;
        private List<ImageBlob> blobs;
    }
}
