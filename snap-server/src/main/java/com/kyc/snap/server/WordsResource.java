package com.kyc.snap.server;

import java.util.List;

import com.google.common.base.Joiner;
import com.kyc.snap.cromulence.CromulenceSolverResult;
import com.kyc.snap.cromulence.NiceCromulenceSolver;
import com.kyc.snap.crossword.Crossword;
import com.kyc.snap.crossword.CrosswordClues;
import com.kyc.snap.crossword.CrosswordParser;
import com.kyc.snap.crossword.WordplaysUtil;
import com.kyc.snap.crossword.WordplaysUtil.ClueSuggestion;
import com.kyc.snap.words.WordsearchSolver;

import lombok.Data;

@Data
public class WordsResource implements WordsService {

    private final WordsearchSolver wordsearchSolver;
    private final CrosswordParser crosswordParser;
    private final NiceCromulenceSolver cromulenceSolver;

    @Override
    public FetchCrosswordClueSuggestionsResponse fetchCrosswordClueSuggestions(FetchCrosswordClueSuggestionsRequest request) {
        List<ClueSuggestion> suggestions = WordplaysUtil.fetchCrosswordClueSuggestions(request.getClue(), request.getNumLetters());
        return new FetchCrosswordClueSuggestionsResponse(suggestions);
    }

    @Override
    public SolveWordsearchResponse solveWordsearch(SolveWordsearchRequest request) {
        List<WordsearchSolver.Result> results = wordsearchSolver.find(request.getGrid(), request.isBoggle());
        return new SolveWordsearchResponse(results);
    }

    @Override
    public FindCrosswordResponse findCrossword(FindCrosswordRequest request) {
        Crossword crossword = crosswordParser.parseCrossword(request.getGrid());
        return new FindCrosswordResponse(crossword);
    }

    @Override
    public ParseCrosswordCluesResponse parseCrosswordClues(ParseCrosswordCluesRequest request) {
        CrosswordClues clues = crosswordParser.parseClues(request.getUnparsedClues());
        return new ParseCrosswordCluesResponse(clues);
    }

    @Override
    public OptimizeCromulenceResponse optimizeCromulence(OptimizeCromulenceRequest request) {
        List<CromulenceSolverResult> results;
        if (request.isCanRearrange()) {
            if (request.getWordLengths() != null)
                results = cromulenceSolver.solveRearrangement(request.getParts(), request.getWordLengths());
            else
                results = cromulenceSolver.solveRearrangement(request.getParts());
        } else {
            if (request.getWordLengths() != null)
                results = cromulenceSolver.solveSlug(Joiner.on("").join(request.getParts()), request.getWordLengths());
            else
                results = cromulenceSolver.solveSlug(Joiner.on("").join(request.getParts()));
        }
        return new OptimizeCromulenceResponse(results);
    }
}
