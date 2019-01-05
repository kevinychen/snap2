package com.kyc.snap.server;

import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.kyc.snap.crossword.WordplaysUtil.ClueSuggestion;
import com.kyc.snap.words.WordsearchSolver;

import lombok.Data;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public interface WordsService {

    @POST
    @Path("words/crossword")
    FetchCrosswordClueSuggestionsResponse fetchCrosswordClueSuggestions(FetchCrosswordClueSuggestionsRequest request);

    public static class FetchCrosswordClueSuggestionsRequest {

        private String clue;
        private int numLetters;

        public String getClue() {
            return clue;
        }

        public int getNumLetters() {
            return numLetters;
        }

        public void setClue(String clue) {
            this.clue = clue;
        }

        public void setNumLetters(int numLetters) {
            this.numLetters = numLetters;
        }
    }

    @Data
    public static class FetchCrosswordClueSuggestionsResponse {

        private final List<ClueSuggestion> suggestions;
    }

    @POST
    @Path("words/search")
    SolveWordsearchResponse solveWordsearch(SolveWordsearchRequest request);

    public static class SolveWordsearchRequest {

        private List<String> grid;

        public List<String> getGrid() {
            return grid;
        }

        public void setGrid(List<String> grid) {
            this.grid = grid;
        }
    }

    @Data
    public static class SolveWordsearchResponse {

        private final Set<WordsearchSolver.Result> results;
    }
}
