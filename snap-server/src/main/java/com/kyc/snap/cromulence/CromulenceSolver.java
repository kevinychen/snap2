package com.kyc.snap.cromulence;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.TreeMultiset;
import com.kyc.snap.antlr.PregexLexer;
import com.kyc.snap.antlr.PregexParser;
import com.kyc.snap.antlr.PregexParser.TermsContext;
import com.kyc.snap.cromulence.TermNodes.ListNode;
import com.kyc.snap.cromulence.TermNodes.SymbolNode;
import com.kyc.snap.cromulence.TermNodes.TermNode;
import com.kyc.snap.cromulence.TermStates.TermState;
import com.kyc.snap.words.DictionaryManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class CromulenceSolver {

    static final int NUM_LETTERS = 26;

    private static final int SEARCH_LIMIT = 1000;
    private static final int NUM_RESULTS = 50;

    private final DictionaryManager dictionaryManager;
    private final Cache<StringPair, double[]> nextLetterFreqsCache;
    private final Cache<String, double[]> singlePrefixFreqsCache;

    public CromulenceSolver(DictionaryManager dictionaryManager) {
        this.dictionaryManager = dictionaryManager;
        this.nextLetterFreqsCache = Caffeine.newBuilder()
            .maximumSize(100000)
            .build();
        this.singlePrefixFreqsCache = Caffeine.newBuilder()
            .maximumSize(100000)
            .build();
    }

    public List<CromulenceSolverResult> solve(String query, @Nullable List<Integer> wordLengths) {
        if (query.length() > 500)
            throw new IllegalArgumentException("Query too long");

        Context context = new Context();
        TermsContext termsContext = new PregexParser(
            new CommonTokenStream(new PregexLexer(CharStreams.fromString(query)))).terms();
        List<TermNode> parts = new ArrayList<>();
        termsContext.term().forEach(childContext -> parts.add(TermNodes.fromAntlr(childContext)));
        parts.add(new SymbolNode(' '));
        Comparator<State> scoreComparator = Comparator.comparingDouble(State::getScore).reversed();
        TermNode node = new ListNode(parts);
        if (node.complexity() > SEARCH_LIMIT)
            throw new IllegalArgumentException("Query too complex");

        TreeMap<Integer, List<State>> statesByLength = new TreeMap<>();
        statesByLength.computeIfAbsent(0, key -> new ArrayList<>()).add(State.builder()
            .termState(node.toTermState(null))
            .words(List.of())
            .prefix("")
            .len(0)
            .quoteLevel(0)
            .score(0.)
            .wordLengths(wordLengths)
            .build());
        TreeMultiset<State> bestFinalStates = TreeMultiset.create(scoreComparator);
        while (!statesByLength.isEmpty()) {
            double minScore = bestFinalStates.size() >= NUM_RESULTS
                ? bestFinalStates.lastEntry().getElement().score
                : Double.NEGATIVE_INFINITY;
            statesByLength.remove(statesByLength.firstKey()).stream().sorted(scoreComparator).limit(SEARCH_LIMIT).forEach(state -> {
                if (state.score < minScore)
                    return;
                for (State newState : state.termState.newStates(state, context))
                    if (newState.termState != null)
                        statesByLength.computeIfAbsent(newState.len, key -> new ArrayList<>()).add(newState);
                    else
                        bestFinalStates.add(newState);
            });
            while (bestFinalStates.size() > NUM_RESULTS)
                bestFinalStates.pollLastEntry();
        }
        return bestFinalStates.stream()
            .limit(NUM_RESULTS)
            .map(state -> new CromulenceSolverResult(state.words, state.score))
            .collect(Collectors.toList());
    }

    @Data
    @Builder(toBuilder = true)
    static class State {
        final TermState termState;
        final List<String> words;
        final String prefix;
        final int len;
        final int quoteLevel;
        final List<Integer> wordLengths;
        final double score;
    }

    @Data
    static class StringPair {
        final String prevWord;
        final String currWord;
    }

    @Data
    class Context {

        double[] getNextLetterProbabilities(List<String> words, String prefix) {
            return nextLetterFreqsCache.get(new StringPair(words.isEmpty() ? null : words.get(words.size() - 1), prefix), key -> {
                double[] freqs = Arrays.copyOf(getCachedFrequencies(prefix), NUM_LETTERS + 1);

                // bias toward words that appear in the biword list after the previous word
                if (key.prevWord != null)
                    updateFrequencies(dictionaryManager.getWordFrequencies(key.prevWord, prefix), prefix, freqs);
                double totalProb = 0;
                for (double prob : freqs)
                    totalProb += prob;
                for (int i = 0; i <= NUM_LETTERS; i++)
                    freqs[i] /= totalProb;
                return freqs;
            });
        }

        double[] getCachedFrequencies(String prefix) {
            return singlePrefixFreqsCache.get(prefix, key -> {
                double[] freqs = new double[NUM_LETTERS + 1];
                updateFrequencies(dictionaryManager.getWordFrequencies(prefix), prefix, freqs);
                return freqs;
            });
        }

        private void updateFrequencies(Map<String, Long> wordFreqs, String prefix, double[] freqs) {
            wordFreqs.forEach((word, frequency) -> {
                if (word.equals(prefix))
                    freqs[NUM_LETTERS] += frequency;
                else
                    freqs[word.charAt(prefix.length()) - 'A'] += frequency;
            });
        }
    }
}
