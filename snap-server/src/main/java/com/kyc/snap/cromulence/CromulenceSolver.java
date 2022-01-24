package com.kyc.snap.cromulence;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.TreeMultiset;
import com.kyc.snap.antlr.PregexLexer;
import com.kyc.snap.antlr.PregexParser;
import com.kyc.snap.antlr.PregexParser.TermsContext;
import com.kyc.snap.cromulence.StateTransitions.TermState;
import com.kyc.snap.cromulence.TermNodes.ListNode;
import com.kyc.snap.cromulence.TermNodes.SymbolNode;
import com.kyc.snap.cromulence.TermNodes.TermNode;
import com.kyc.snap.words.DictionaryManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import one.util.streamex.StreamEx;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class CromulenceSolver {

    static final int NUM_LETTERS = 26;

    private static final int SEARCH_LIMIT = 2000;
    private static final int NUM_RESULTS = 100;

    private final DictionaryManager dictionaryManager;
    private final Cache<NextLetterFreqsKey, double[]> nextLetterFreqsCache;
    private final Cache<SinglePrefixFreqsKey, double[]> singlePrefixFreqsCache;
    private final Random random = new Random(0);

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
        for (int i = 0; i < 1000 && !statesByLength.isEmpty(); i++) {
            List<State> currStates = statesByLength.remove(statesByLength.firstKey());
            double scoreThreshold = Math.max(approxScoreThreshold(currStates), minScoreThreshold(bestFinalStates));
            for (State state : currStates)
                if (state.score >= scoreThreshold)
                    for (State newState : state.termState.newStates(state, context))
                        if (newState.termState != null)
                            statesByLength.computeIfAbsent(newState.len, key -> new ArrayList<>()).add(newState);
                        else
                            bestFinalStates.add(newState);
            while (bestFinalStates.size() > NUM_RESULTS)
                bestFinalStates.pollLastEntry();
        }
        return StreamEx.of(bestFinalStates.stream())
            .distinct(State::getWords)
            .map(state -> new CromulenceSolverResult(state.words, state.score))
            .collect(Collectors.toList());
    }

    private double approxScoreThreshold(List<State> states) {
        if (states.size() < SEARCH_LIMIT)
            return Double.NEGATIVE_INFINITY;
        double[] samples = new double[SEARCH_LIMIT];
        for (int i = 0; i < SEARCH_LIMIT; i++)
            samples[i] = states.get(random.nextInt(states.size())).score;
        Arrays.sort(samples);
        return samples[SEARCH_LIMIT - SEARCH_LIMIT * SEARCH_LIMIT / states.size()];
    }

    private double minScoreThreshold(TreeMultiset<State> bestFinalStates) {
        if (bestFinalStates.size() < NUM_RESULTS)
            return Double.NEGATIVE_INFINITY;
        return bestFinalStates.lastEntry().getElement().score;
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
    static class NextLetterFreqsKey {
        final String prevWord;
        final String prefix;
        final Integer wordLength;
    }

    @Data
    static class SinglePrefixFreqsKey {
        final String prefix;
        final Integer wordLength;
    }

    @Data
    class Context {

        double[] getNextLetterProbabilities(List<String> words, String prefix, List<Integer> wordLengths) {
            String prevWord = words.isEmpty() ? null : words.get(words.size() - 1);
            Integer wordLength = wordLengths == null ? null : wordLengths.get(0);
            return nextLetterFreqsCache.get(new NextLetterFreqsKey(prevWord, prefix, wordLength), key -> {
                double[] freqs = Arrays.copyOf(getCachedFrequencies(prefix, wordLength), NUM_LETTERS + 1);

                // bias toward words that appear in the biword list after the previous word
                if (prevWord != null)
                    updateFrequencies(dictionaryManager.getWordFrequencies(prevWord, prefix), prefix, wordLength, freqs);
                double totalProb = 0;
                for (double prob : freqs)
                    totalProb += prob;
                for (int i = 0; i <= NUM_LETTERS; i++)
                    freqs[i] /= totalProb;
                return freqs;
            });
        }

        double[] getCachedFrequencies(String prefix, Integer wordLength) {
            return singlePrefixFreqsCache.get(new SinglePrefixFreqsKey(prefix, wordLength), key -> {
                double[] freqs = new double[NUM_LETTERS + 1];
                updateFrequencies(dictionaryManager.getWordFrequencies(prefix), prefix, wordLength, freqs);
                return freqs;
            });
        }

        private void updateFrequencies(Map<String, Long> wordFreqs, String prefix, Integer wordLength, double[] freqs) {
            wordFreqs.forEach((word, frequency) -> {
                if (wordLength != null && word.length() != wordLength)
                    return;
                if (word.equals(prefix))
                    freqs[NUM_LETTERS] += frequency;
                else
                    freqs[word.charAt(prefix.length()) - 'A'] += frequency;
            });
        }
    }
}
