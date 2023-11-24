package com.kyc.snap.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.glassfish.jersey.media.multipart.FormDataParam;

import com.kyc.snap.crossword.Crossword;
import com.kyc.snap.crossword.CrosswordClues;
import com.kyc.snap.crossword.CrosswordFormula;
import com.kyc.snap.document.Document;
import com.kyc.snap.document.Section;
import com.kyc.snap.grid.Grid;
import com.kyc.snap.grid.GridLines;
import com.kyc.snap.grid.GridPosition;
import com.kyc.snap.image.ImageBlob;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/documents")
public interface DocumentService {

    @GET
    @Path("/{documentId}")
    @Produces(MediaType.APPLICATION_JSON)
    Document getDocument(@PathParam("documentId") String documentId);

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
    Document createDocumentFromUrl(CreateDocumentFromUrlRequest request) throws Exception;

    record CreateDocumentFromUrlRequest(String url) {}

    @POST
    @Path("/{documentId}/lines")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    GridLines findGridLines(@PathParam("documentId") String documentId, FindGridLinesRequest request);

    record FindGridLinesRequest(Section section, FindGridLinesMode findGridLinesMode, boolean interpolate) {}

    enum FindGridLinesMode {

        EXPLICIT,
        IMPLICIT,
    }

    @POST
    @Path("/{documentId}/blobs")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    List<ImageBlob> findBlobs(@PathParam("documentId") String documentId, FindBlobsRequest request);

    record FindBlobsRequest(Section section, int minBlobSize, boolean exact) {}

    @POST
    @Path("/{documentId}/grid")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    FindGridResponse findGrid(@PathParam("documentId") String documentId, FindGridRequest request);

    record FindGridRequest(Section section, GridLines gridLines) {}

    record FindGridResponse(GridPosition gridPosition, Grid grid) {}

    @POST
    @Path("/{documentId}/clues")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    FindCrosswordCluesResponse findCrosswordClues(
            @PathParam("documentId") String documentId, FindCrosswordCluesRequest request);

    record FindCrosswordCluesRequest(Crossword crossword) {}

    record FindCrosswordCluesResponse(CrosswordClues clues, List<CrosswordFormula> formulas) {}

    @POST
    @Path("/{documentId}/export/sheet/{spreadsheetId}/{sheetId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    boolean exportSheet(@PathParam("documentId") String documentId, @PathParam("spreadsheetId") String spreadsheetId,
            @PathParam("sheetId") int sheetId, ExportSheetRequest request);

    record ExportSheetRequest(
            Marker marker,
            Section section,
            GridLines gridLines,
            Grid grid,
            Crossword crossword,
            CrosswordClues crosswordClues,
            List<ImageBlob> blobs) {}

    record Marker(int row, int col) {}

    @POST
    @Path("/{documentId}/export/slide/{presentationId}/{slideId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    boolean exportSlide(@PathParam("documentId") String documentId, @PathParam("presentationId") String presentationId,
            @PathParam("slideId") String slideId, ExportSlideRequest request);

    record ExportSlideRequest(Section section, List<ImageBlob> blobs) {}
}
