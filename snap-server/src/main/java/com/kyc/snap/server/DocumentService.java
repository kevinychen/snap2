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

@Path("/documents")
public interface DocumentService {

    @POST
    @Path("/pdf")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    Document createDocumentFromPdf(@FormDataParam("pdf") InputStream pdf) throws IOException;

    @POST
    @Path("/sheets/{spreadsheetId}/{sheetId}")
    @Produces(MediaType.APPLICATION_JSON)
    Point initializeSheet(@PathParam("spreadsheetId") String spreadsheetId, @PathParam("sheetId") int sheetId);
}
