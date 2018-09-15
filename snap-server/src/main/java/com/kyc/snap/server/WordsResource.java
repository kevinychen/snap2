package com.kyc.snap.server;

import java.util.List;

import com.kyc.snap.crossword.WordplaysUtil;
import com.kyc.snap.crossword.WordplaysUtil.ClueSuggestion;
import com.kyc.snap.words.TrigramPuzzleSolver;

import lombok.Data;

@Data
public class WordsResource implements WordsService {

    private final TrigramPuzzleSolver trigramPuzzleSolver;

    @Override
    public SolveTrigramPuzzleResponse solveTrigramPuzzle(SolveTrigramPuzzleRequest request) {
        List<String> solution = trigramPuzzleSolver.solve(request.getTrigrams(), request.getWordLengths());
        return new SolveTrigramPuzzleResponse(solution);
    }

    @Override
    public FetchCrosswordClueSuggestionsResponse fetchCrosswordClueSuggestions(FetchCrosswordClueSuggestionsRequest request) {
        List<ClueSuggestion> suggestions = WordplaysUtil.fetchCrosswordClueSuggestions(request.getClue(), request.getNumLetters());
        return new FetchCrosswordClueSuggestionsResponse(suggestions);
    }
}
