package com.kyc.snap.api;

import java.util.List;
import java.util.Set;

import com.kyc.snap.crossword.Crossword;
import com.kyc.snap.crossword.CrosswordClues;
import com.kyc.snap.crossword.CrosswordFormula;
import com.kyc.snap.grid.Grid;
import com.kyc.snap.solver.GenericSolver;
import com.kyc.snap.words.WordSearchSolver;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/")
public interface WordsService {

    @POST
    @Path("words/search")
    SolveWordSearchResponse solveWordSearch(SolveWordSearchRequest request);

    record SolveWordSearchRequest(
            List<String> grid, Set<String> wordBank, boolean boggle, boolean fuzzy) {}

    record SolveWordSearchResponse(List<WordSearchSolver.Result> results, boolean hitLimit) {}

    @POST
    @Path("words/findCrossword")
    FindCrosswordResponse findCrossword(FindCrosswordRequest request);

    record FindCrosswordRequest(Grid grid) {}

    record FindCrosswordResponse(Crossword crossword) {}

    @POST
    @Path("words/parseCrosswordClues")
    ParseCrosswordCluesResponse parseCrosswordClues(ParseCrosswordCluesRequest request);

    record ParseCrosswordCluesRequest(String unparsedClues) {}

    record ParseCrosswordCluesResponse(CrosswordClues clues) {}

    @POST
    @Path("words/crosswordFormulas")
    GetCrosswordFormulasResponse getCrosswordFormulas(GetCrosswordFormulasRequest request);

    record GetCrosswordFormulasRequest(Crossword crossword, CrosswordClues clues) {}

    record GetCrosswordFormulasResponse(List<CrosswordFormula> formulas) {}

    @POST
    @Path("words/pregex")
    PregexResponse pregex(PregexRequest request);

    record PregexRequest(List<String> parts, boolean canRearrange, List<Integer> wordLengths) {}

    record PregexResponse(List<GenericSolver.Result> results) {}

    @POST
    @Path("words/find")
    FindWordsResponse findWords(FindWordsRequest request);

    record FindWordsRequest(
            Integer minLength,
            Integer maxLength,
            Long minFreq,
            String regex,
            String containsSubsequence,
            String containedSubsequence,
            String contains,
            String contained) {}

    record FindWordsResponse(List<String> words) {}
}
