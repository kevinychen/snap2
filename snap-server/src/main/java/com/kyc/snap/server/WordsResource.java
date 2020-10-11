package com.kyc.snap.server;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.kyc.snap.api.WordsService;
import com.kyc.snap.cromulence.CromulenceSolverResult;
import com.kyc.snap.cromulence.NiceCromulenceSolver;
import com.kyc.snap.crossword.Crossword;
import com.kyc.snap.crossword.CrosswordClues;
import com.kyc.snap.crossword.CrosswordParser;
import com.kyc.snap.crossword.WordplaysUtil;
import com.kyc.snap.crossword.WordplaysUtil.ClueSuggestion;
import com.kyc.snap.wikinet.Wikinet;
import com.kyc.snap.words.DictionaryManager;
import com.kyc.snap.words.StringUtil;
import com.kyc.snap.words.WordsearchSolver;

import lombok.Data;
import one.util.streamex.EntryStream;

@Data
public class WordsResource implements WordsService {

    private final WordsearchSolver wordsearchSolver;
    private final CrosswordParser crosswordParser;
    private final NiceCromulenceSolver cromulenceSolver;
    private final DictionaryManager dictionary;
    private final Wikinet wikinet;

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

    @Override
    public FindWordsResponse findWords(FindWordsRequest request) {
        EntryStream<String, Long> frequencies;
        if (request.getDictionary() == Dictionary.NORMAL)
            frequencies = EntryStream.of(dictionary.getWordFrequencies());
        else if (request.getLengthPattern() != null)
            frequencies = wikinet.getCleanedTitlesWithFrequencies();
        else
            frequencies = wikinet.getLetterOnlyTitlesWithFrequencies();

        String regex = request.getRegex() == null ? null : request.getRegex().toUpperCase();
        String containsSubseq = request.getContainsSubseq() == null ? null : clean(request.getContainsSubseq());
        String containedSubseq = request.getContainedSubseq() == null ? null : clean(request.getContainedSubseq());
        String contains = request.getContains() == null ? null : StringUtil.sorted(clean(request.getContains()));
        String contained = request.getContained() == null ? null : StringUtil.sorted(clean(request.getContained()));
        List<String> words = frequencies
            .filterKeys(word -> request.getMinLength() == null || word.length() >= request.getMinLength())
            .filterKeys(word -> request.getMaxLength() == null || word.length() <= request.getMaxLength())
            .filterValues(freq -> request.getMinFreq() == null || freq >= request.getMinFreq())
            .mapKeys(String::toUpperCase)
            .filterKeys(word -> regex == null || word.matches(regex))
            .filterKeys(word -> containsSubseq == null || StringUtil.isSubsequence(word, containsSubseq))
            .filterKeys(word -> containedSubseq == null || StringUtil.isSubsequence(containedSubseq, word))
            .filterKeys(word -> contains == null || StringUtil.isSubsequence(StringUtil.sorted(word), contains))
            .filterKeys(word -> contained == null || StringUtil.isSubsequence(contained, StringUtil.sorted(word)))
            .filterKeys(word -> request.getLengthPattern() == null ||
                    Arrays.stream(word.split(" ")).map(String::length).collect(Collectors.toList()).equals(request.getLengthPattern()))
            .keys()
            .limit(100)
            .toList();
        frequencies.close();
        return new FindWordsResponse(words);
    }

    private String clean(String s) {
        return s.toUpperCase().replaceAll("[^A-Z]", "");
    }
}
