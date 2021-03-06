package com.kyc.snap.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.kyc.snap.cromulence.CromulenceSolverResult;
import com.kyc.snap.crossword.Crossword;
import com.kyc.snap.crossword.CrosswordClues;
import com.kyc.snap.crossword.WordplaysUtil.ClueSuggestion;
import com.kyc.snap.grid.Grid;
import com.kyc.snap.words.WordsearchSolver;

import lombok.Data;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public interface WordsService {

    @POST
    @Path("words/crossword")
    FetchCrosswordClueSuggestionsResponse fetchCrosswordClueSuggestions(FetchCrosswordClueSuggestionsRequest request);

    @Data
    public static class FetchCrosswordClueSuggestionsRequest {

        private final String clue;
        private final int numLetters;
    }

    @Data
    public static class FetchCrosswordClueSuggestionsResponse {

        private final List<ClueSuggestion> suggestions;
    }

    @POST
    @Path("words/search")
    SolveWordsearchResponse solveWordsearch(SolveWordsearchRequest request);

    @Data
    public static class SolveWordsearchRequest {

        private List<String> grid;
        private boolean boggle = false;
    }

    @Data
    public static class SolveWordsearchResponse {

        private final List<WordsearchSolver.Result> results;
    }

    @POST
    @Path("words/findCrossword")
    FindCrosswordResponse findCrossword(FindCrosswordRequest request);

    @Data
    public static class FindCrosswordRequest {

        private final Grid grid;
    }

    @Data
    public static class FindCrosswordResponse {

        private final Crossword crossword;
    }

    @POST
    @Path("words/parseCrosswordClues")
    ParseCrosswordCluesResponse parseCrosswordClues(ParseCrosswordCluesRequest request);

    @Data
    public static class ParseCrosswordCluesRequest {

        private final String unparsedClues;
    }

    @Data
    public static class ParseCrosswordCluesResponse {

        private final CrosswordClues clues;
    }

    @POST
    @Path("words/cromulence")
    OptimizeCromulenceResponse optimizeCromulence(OptimizeCromulenceRequest request);

    @Data
    public static class OptimizeCromulenceRequest {

        private final List<String> parts;
        private boolean canRearrange;
        private List<Integer> wordLengths;
    }

    @Data
    public static class OptimizeCromulenceResponse {

        private final List<CromulenceSolverResult> results;
    }

    @POST
    @Path("words/find")
    FindWordsResponse findWords(FindWordsRequest request);

    @Data
    public static class FindWordsRequest {

        private final Dictionary dictionary;
        private Integer minLength;
        private Integer maxLength;
        private Long minFreq;
        private String regex;
        private String containsSubseq;
        private String containedSubseq;
        private String contains;
        private String contained;
        private List<Integer> lengthPattern; // WIKIPEDIA_TITLES only
    }

    public enum Dictionary {
        NORMAL, WIKIPEDIA_TITLES;
    }

    @Data
    public static class FindWordsResponse {

        private final List<String> words;
    }
}
