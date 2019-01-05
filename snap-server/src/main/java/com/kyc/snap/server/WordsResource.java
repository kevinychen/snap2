package com.kyc.snap.server;

import java.util.List;
import java.util.Set;

import com.kyc.snap.crossword.WordplaysUtil;
import com.kyc.snap.crossword.WordplaysUtil.ClueSuggestion;
import com.kyc.snap.words.WordsearchSolver;

import lombok.Data;

@Data
public class WordsResource implements WordsService {

    private final WordsearchSolver wordsearchSolver;

    @Override
    public FetchCrosswordClueSuggestionsResponse fetchCrosswordClueSuggestions(FetchCrosswordClueSuggestionsRequest request) {
        List<ClueSuggestion> suggestions = WordplaysUtil.fetchCrosswordClueSuggestions(request.getClue(), request.getNumLetters());
        return new FetchCrosswordClueSuggestionsResponse(suggestions);
    }

    @Override
    public SolveWordsearchResponse solveWordsearch(SolveWordsearchRequest request) {
        Set<WordsearchSolver.Result> results = wordsearchSolver.find(request.getGrid());
        return new SolveWordsearchResponse(results);
    }
}
