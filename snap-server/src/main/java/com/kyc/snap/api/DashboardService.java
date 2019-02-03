
package com.kyc.snap.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.collect.ImmutableList;

import lombok.Data;

@Path("/dashboard")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface DashboardService {

    @Data
    public static class InitializeDashboardRequest {

        private final String serverOrigin;
        private final String name;
        private final String folderId;
    }

    @POST
    @Path("/{dashboardId}/generate")
    GenerateSheetResponse generateSheet(@PathParam("dashboardId") String dashboardId, GenerateSheetRequest request);

    @Data
    public static class GenerateSheetRequest {

        private final String puzzleUrl;
        private String title = "";
        private String slackToken = "";
        private List<String> slackEmailAddresses = ImmutableList.of();
    }

    @Data
    public static class GenerateSheetResponse {

        private final String title;
        private final String sheetUrl;
        private final String slackLink;
    }

    @POST
    @Path("/{dashboardId}/solve")
    boolean solvePuzzle(@PathParam("dashboardId") String dashboardId, SolvePuzzleRequest request);

    @Data
    public static class SolvePuzzleRequest {

        private final String answer;
        private final String sheetId;
        private final String title;
        private String slackToken = "";
        private String slackLink = "";
    }
}
