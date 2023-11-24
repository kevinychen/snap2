package com.kyc.snap.server;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.kyc.snap.api.WordsService;
import com.kyc.snap.crossword.Crossword;
import com.kyc.snap.crossword.CrosswordClues;
import com.kyc.snap.crossword.CrosswordFormula;
import com.kyc.snap.crossword.CrosswordParser;
import com.kyc.snap.solver.GenericSolver;
import com.kyc.snap.solver.PregexSolver;
import com.kyc.snap.words.EnglishDictionary;
import com.kyc.snap.words.StringUtil;
import com.kyc.snap.words.WordsearchSolver;

import lombok.Data;
import one.util.streamex.EntryStream;

@Data
public class WordsResource implements WordsService {

    private final WordsearchSolver wordsearchSolver;
    private final CrosswordParser crosswordParser;
    private final PregexSolver pregexSolver;
    private final EnglishDictionary dictionary;

    @Override
    public SolveWordsearchResponse solveWordsearch(SolveWordsearchRequest request) {
        List<WordsearchSolver.Result> results = wordsearchSolver.find(request.getGrid(), request.getWordbank(), request.isBoggle());
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
    public GetCrosswordFormulasResponse getCrosswordFormulas(GetCrosswordFormulasRequest request) {
        List<CrosswordFormula> formulas = crosswordParser
            .getFormulas(request.getCrossword(), request.getClues());
        return new GetCrosswordFormulasResponse(formulas);
    }

    @Override
    public PregexResponse pregex(PregexRequest request) {
        String query;
        if (request.isCanRearrange()) {
            if (request.getParts().size() == 1)
                query = "<" + request.getParts().get(0) + ">";
            else
                query = "<" + Joiner.on("").join(request.getParts().stream()
                    .map(part -> "(" + part + ")")
                    .collect(Collectors.toList())) + ">";
        } else
            query = Joiner.on("").join(request.getParts());
        List<GenericSolver.Result> results = pregexSolver.solve(query, request.getWordLengths());
        return new PregexResponse(results);
    }

    @Override
    public FindWordsResponse findWords(FindWordsRequest request) {

        String regex = request.getRegex() == null ? null : request.getRegex().toUpperCase();
        String containsSubseq = request.getContainsSubseq() == null ? null : clean(request.getContainsSubseq());
        String containedSubseq = request.getContainedSubseq() == null ? null : clean(request.getContainedSubseq());
        String contains = request.getContains() == null ? null : StringUtil.sorted(clean(request.getContains()));
        String contained = request.getContained() == null ? null : StringUtil.sorted(clean(request.getContained()));
        List<String> words = EntryStream.of(dictionary.getWordFrequencies())
            .filterKeys(word -> request.getMinLength() == null || word.length() >= request.getMinLength())
            .filterKeys(word -> request.getMaxLength() == null || word.length() <= request.getMaxLength())
            .filterValues(freq -> request.getMinFreq() == null || freq >= request.getMinFreq())
            .mapKeys(String::toUpperCase)
            .filterKeys(word -> regex == null || word.matches(regex))
            .filterKeys(word -> containsSubseq == null || StringUtil.isSubsequence(word, containsSubseq))
            .filterKeys(word -> containedSubseq == null || StringUtil.isSubsequence(containedSubseq, word))
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
