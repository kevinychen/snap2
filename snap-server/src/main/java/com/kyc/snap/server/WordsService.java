package com.kyc.snap.server;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import lombok.Data;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public interface WordsService {

    @POST
    @Path("words/trigram")
    SolveTrigramPuzzleResponse solveTrigramPuzzle(SolveTrigramPuzzleRequest request);

    @Data
    public static class SolveTrigramPuzzleRequest {

        private final List<String> trigrams;
        private final List<Integer> wordLengths;
    }

    @Data
    public static class SolveTrigramPuzzleResponse {

        private final List<String> solution;
    }
}
