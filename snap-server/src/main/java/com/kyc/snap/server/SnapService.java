
package com.kyc.snap.server;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.FormDataParam;

import com.fasterxml.jackson.annotation.JsonValue;
import com.kyc.snap.grid.Grid;
import com.kyc.snap.grid.GridLines;
import com.kyc.snap.grid.GridPosition;

import lombok.Data;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public interface SnapService {

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

    @POST
    @Path("session/{sessionId}/grid/colors")
    Grid findGridColors(@PathParam("sessionId") String sessionId);

    @POST
    @Path("session/{sessionId}/grid/text")
    Grid findGridText(@PathParam("sessionId") String sessionId);

    @POST
    @Path("session/{sessionId}/grid/borders")
    Grid findGridBorders(@PathParam("sessionId") String sessionId);

    /**
     * Exports the current session data to a Google sheet.
     *
     * @param sessionId
     * @return the spreadsheet URL
     */
    @POST
    @Path("session/{sessionId}/spreadsheet")
    StringJson exportToSpreadsheet(@PathParam("sessionId") String sessionId);

    @Data
    public static class StringJson {

        @JsonValue
        public final String value;
    }
}
