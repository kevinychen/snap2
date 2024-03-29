package com.kyc.snap.server;

import java.util.List;
import java.util.stream.Collectors;

import com.kyc.snap.api.WordsService;
import com.kyc.snap.crossword.Crossword;
import com.kyc.snap.crossword.CrosswordClues;
import com.kyc.snap.crossword.CrosswordFormula;
import com.kyc.snap.crossword.CrosswordParser;
import com.kyc.snap.solver.GenericSolver;
import com.kyc.snap.solver.PregexSolver;
import com.kyc.snap.words.CustomDictionary;
import com.kyc.snap.words.Dictionary;
import com.kyc.snap.words.StringUtil;
import com.kyc.snap.words.WordSearchSolver;
import com.kyc.snap.words.WordSearchSolver.AllResults;

import one.util.streamex.EntryStream;

public record WordsResource(
        WordSearchSolver wordsearchSolver,
        CrosswordParser crosswordParser,
        PregexSolver pregexSolver,
        Dictionary dictionary) implements WordsService {

    @Override
    public SolveWordSearchResponse solveWordSearch(SolveWordSearchRequest request) {
        List<Integer> wordLengths;
        if (!request.wordBank().isEmpty())
            wordLengths = request.fuzzy() ? List.of(0, 0) : List.of(0);
        else if (request.boggle())
            wordLengths = request.fuzzy() ? List.of(5, 7) : List.of(5);
        else
            wordLengths = request.fuzzy() ? List.of(3, 5) : List.of(3);

        AllResults results = wordsearchSolver.find(
                request.grid(),
                request.wordBank().isEmpty() ? dictionary : new CustomDictionary(request.wordBank()),
                request.boggle(),
                wordLengths);
        return new SolveWordSearchResponse(results.results(), results.hitLimit());
    }

    @Override
    public FindCrosswordResponse findCrossword(FindCrosswordRequest request) {
        Crossword crossword = crosswordParser.parseCrossword(request.grid());
        return new FindCrosswordResponse(crossword);
    }

    @Override
    public ParseCrosswordCluesResponse parseCrosswordClues(ParseCrosswordCluesRequest request) {
        CrosswordClues clues = crosswordParser.parseClues(request.unparsedClues());
        return new ParseCrosswordCluesResponse(clues);
    }

    @Override
    public GetCrosswordFormulasResponse getCrosswordFormulas(GetCrosswordFormulasRequest request) {
        List<CrosswordFormula> formulas = crosswordParser
                .getFormulas(request.crossword(), request.clues());
        return new GetCrosswordFormulasResponse(formulas);
    }

    @Override
    public PregexResponse pregex(PregexRequest request) {
        String query;
        if (request.canRearrange()) {
            if (request.parts().size() == 1)
                query = "<" + request.parts().get(0) + ">";
            else
                query = request.parts().stream()
                        .map(part -> "(" + part + ")")
                        .collect(Collectors.joining("", "<", ">"));
        } else
            query = String.join("", request.parts());
        List<GenericSolver.Result> results = pregexSolver.solve(query, request.wordLengths());
        return new PregexResponse(results);
    }

    @Override
    public FindWordsResponse findWords(FindWordsRequest request) {
        String regex = request.regex() == null ? null : request.regex().toUpperCase();
        String containsSubsequence = request.containsSubsequence() == null ? null : clean(request.containsSubsequence());
        String containedSubsequence = request.containedSubsequence() == null ? null : clean(request.containedSubsequence());
        String contains = request.contains() == null ? null : StringUtil.sorted(clean(request.contains()));
        String contained = request.contained() == null ? null : StringUtil.sorted(clean(request.contained()));
        List<String> words = EntryStream.of(dictionary.getWordFrequencies())
                .filterKeys(word -> request.minLength() == null || word.length() >= request.minLength())
                .filterKeys(word -> request.maxLength() == null || word.length() <= request.maxLength())
                .filterValues(freq -> request.minFreq() == null || freq >= request.minFreq())
                .mapKeys(String::toUpperCase)
                .filterKeys(word -> regex == null || word.matches(regex))
                .filterKeys(word -> containsSubsequence == null || StringUtil.isSubsequence(word, containsSubsequence))
                .filterKeys(word -> containedSubsequence == null || StringUtil.isSubsequence(containedSubsequence, word))
                .filterKeys(word -> contains == null || StringUtil.isSubsequence(StringUtil.sorted(word), contains))
                .filterKeys(word -> contained == null || StringUtil.isSubsequence(contained, StringUtil.sorted(word)))
                .keys()
                .limit(100)
                .toList();
        return new FindWordsResponse(words);
    }

    private String clean(String s) {
        return s.toUpperCase().replaceAll("[^A-Z]", "");
    }
}
