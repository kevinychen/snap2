package com.kyc.snap.server;

import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.FormDataParam;

import com.kyc.snap.document.Document;
import com.kyc.snap.document.Rectangle;
import com.kyc.snap.grid.Grid;
import com.kyc.snap.grid.GridLines;
import com.kyc.snap.grid.GridPosition;

import lombok.Data;

@Path("/documents")
public interface DocumentService {

    @POST
    @Path("/pdf")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    Document createDocumentFromPdf(@FormDataParam("pdf") InputStream pdf) throws IOException;

    @POST
    @Path("/url")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Document createDocumentFromUrl(CreateDocumentFromUrlRequest url) throws IOException;

    @Data
    public static class CreateDocumentFromUrlRequest {

        private final String url;
    }

    @POST
    @Path("/sheets/{spreadsheetId}/{sheetId}")
    @Produces(MediaType.APPLICATION_JSON)
    Point initializeSheet(@PathParam("spreadsheetId") String spreadsheetId, @PathParam("sheetId") int sheetId);

    @POST
    @Path("/{documentId}/lines")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    FindGridLinesResponse findGridLines(@PathParam("documentId") String documentId, Section section);

    @Data
    public static class Section {

        private final int page;
        private final Rectangle rectangle;
    }

    @Data
    public static class FindGridLinesResponse {

        private final GridLines gridLines;
        private final GridPosition gridPosition;
    }

    @POST
    @Path("/{documentId}/grid")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Grid findGrid(@PathParam("documentId") String documentId, FindGridRequest request);

    @Data
    public static class FindGridRequest {

        private final Section section;
        private final GridPosition gridPosition;
        private boolean findColors = false;
        private boolean findBorders = false;
    }

    @POST
    @Path("/{documentId}/export/{spreadsheetId}/{sheetId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    Point export(@PathParam("documentId") String documentId, @PathParam("spreadsheetId") String spreadsheetId,
            @PathParam("sheetId") int sheetId, ExportRequest request);

    @Data
    public static class ExportRequest {

        private final Section section;
        private GridPosition gridPosition;
        private Grid grid;
    }
}
