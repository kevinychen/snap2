
package com.kyc.snap.server;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.FormDataParam;

import com.fasterxml.jackson.annotation.JsonValue;
import com.kyc.snap.crossword.Crossword;
import com.kyc.snap.crossword.CrosswordClues;
import com.kyc.snap.grid.Grid;
import com.kyc.snap.grid.GridLines;
import com.kyc.snap.grid.GridPosition;
import com.kyc.snap.server.ImageSession.Parameters;

import lombok.Data;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public interface ImageService {

    /**
     * Creates a new session for the given image.
     *
     * @param fileInputStream
     *            binary contents of image
     * @return the session ID to use for subsequent requests
     */
    @POST
    @Path("session")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    StringJson createImageSession(@FormDataParam("image") InputStream imageStream) throws IOException;

    @POST
    @Path("session/{sessionId}/parameters")
    Parameters setParameters(@PathParam("sessionId") String sessionId, @QueryParam("approxGridSize") Integer approxGridSize);

    @POST
    @Path("session/{sessionId}/lines")
    GridLines findGridLines(@PathParam("sessionId") String sessionId);

    @POST
    @Path("session/{sessionId}/lines/interpolate")
    GridLines getInterpolatedGridLines(@PathParam("sessionId") String sessionId);

    @POST
    @Path("session/{sessionId}/lines/implicit")
    GridLines findImplicitGridLines(@PathParam("sessionId") String sessionId);

    @POST
    @Path("session/{sessionId}/position")
    GridPosition getGridPosition(@PathParam("sessionId") String sessionId);

    @GET
    @Path("session/{sessionId}/images/{row}/{col}.png")
    @Produces("image/png")
    BufferedImage getSubimage(@PathParam("sessionId") String sessionId, @PathParam("row") int row, @PathParam("col") int col);

    @POST
    @Path("session/{sessionId}/grid/colors")
    Grid findGridColors(@PathParam("sessionId") String sessionId);

    @POST
    @Path("session/{sessionId}/grid/text")
    Grid findGridText(@PathParam("sessionId") String sessionId);

    @POST
    @Path("session/{sessionId}/grid/borders")
    Grid findGridBorders(@PathParam("sessionId") String sessionId);

    @POST
    @Path("session/{sessionId}/spreadsheet/grid")
    StringJson exportGridToSpreadsheet(@PathParam("sessionId") String sessionId);

    @POST
    @Path("session/{sessionId}/spreadsheet/images")
    StringJson exportImagesToSpreadsheet(@PathParam("sessionId") String sessionId);

    @POST
    @Path("session/{sessionId}/crossword")
    Crossword parseCrossword(@PathParam("sessionId") String sessionId);

    @POST
    @Path("session/{sessionId}/crossword/clues")
    CrosswordClues parseCrosswordClues(@PathParam("sessionId") String sessionId, @QueryParam("clues") String unparsedClues);

    @POST
    @Path("session/{sessionId}/spreadsheet/crossword")
    StringJson exportCrosswordToSpreadsheet(@PathParam("sessionId") String sessionId);

    @Data
    public static class StringJson {

        @JsonValue
        private final String value;
    }
}
