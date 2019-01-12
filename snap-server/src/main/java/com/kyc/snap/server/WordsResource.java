package com.kyc.snap.server;

import java.util.List;
import java.util.Set;

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
}
