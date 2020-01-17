package com.kyc.snap.cromulence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.kyc.snap.words.DictionaryManager;

import lombok.Builder;
import lombok.Data;

/**
 * Exposes a low-level API to optimize cromulence of a given problem. The API allows full
 * flexibility of how to describe the restrictions under which to maximize cromulence. Use
 * {@link CromulenceSolver} and {@link NiceCromulenceSolver} for easier-to-use, but less flexible,
 * APIs.
 */
@Data
public class LowLevelCromulenceSolver {

    private static final int SEARCH_LIMIT = 1000;
    private static final int NUM_RESULTS = 50;

    private final DictionaryManager dictionary;
    private final Map<String, double[]> nextLetterFreqsCache = new HashMap<>();

    public <State> List<CromulenceSolverResult> solve(CromulenceSolverInput<State> input) {
        List<Candidate<State>> candidates = ImmutableList.of(Candidate.<State> builder()
            .words(ImmutableList.of())
            .currentPrefix("")
            .score(0)
            .state(input.initialState())
            .build());
        for (int n = 0; n < input.getNumEmissions(); n++) {
            List<Candidate<State>> newCandidates = new ArrayList<>();
            for (Candidate<State> candidate : candidates) {
                double[] nextLetterProbs = getNextLetterProbabilities(candidate.words, candidate.currentPrefix);
                for (EmissionAndNewState<State> emission : input.getNextEmissions(candidate.state))
                    for (int j = 0; j < Emission.SIZE; j++) {
                        double prob = nextLetterProbs[j] * emission.getEmission().getProbs()[j];
                        if (prob > 0) {
                            Candidate.CandidateBuilder<State> newCandidate = Candidate.<State> builder()
                                .score(candidate.score + Math.log(prob))
                                .state(emission.getState());
                            if (j >= Emission.NUM_LETTERS) {
                                newCandidate
                                    .words(ImmutableList.<String> builder()
                                        .addAll(candidate.words)
                                        .add(candidate.currentPrefix + (char) ('A' + j % Emission.NUM_LETTERS))
                                        .build())
                                    .currentPrefix("");
                            } else {
                                newCandidate.words(candidate.words).currentPrefix(candidate.currentPrefix + (char) ('A' + j));
                            }
                            newCandidates.add(newCandidate.build());
                        }
                    }
            }
            candidates = newCandidates.stream()
                .sorted(Comparator.comparingDouble(candidate -> -candidate.score))
                .limit(SEARCH_LIMIT)
                .collect(Collectors.toList());
        }
        return candidates.stream()
                .limit(NUM_RESULTS)
                .map(candidate -> new CromulenceSolverResult(candidate.words, candidate.score))
                .collect(Collectors.toList());
    }

    private double[] getNextLetterProbabilities(List<String> words, String prefix) {
        double[] freqs = Arrays.copyOf(getCachedFrequencies(prefix), Emission.SIZE);
        // bias toward words that appear in the biword list after the previous word
        // TODO use n-grams for better probability estimation for words not in the dictionary
        if (!words.isEmpty())
            updateFrequencies(dictionary.getWordFrequencies(words.get(words.size() - 1), prefix), prefix, freqs);
        double totalProb = 0;
        for (double prob : freqs)
            totalProb += prob;
        for (int i = 0; i < freqs.length; i++)
            freqs[i] /= totalProb;
        return freqs;
    }

    private double[] getCachedFrequencies(String prefix) {
        if (nextLetterFreqsCache.containsKey(prefix))
            return nextLetterFreqsCache.get(prefix);
        double[] freqs = new double[Emission.SIZE];
        updateFrequencies(dictionary.getWordFrequencies(prefix), prefix, freqs);
        if (prefix.length() <= 2)
            nextLetterFreqsCache.put(prefix, freqs);
        return freqs;
    }

    private static void updateFrequencies(Map<String, Long> wordFreqs, String prefix, double[] freqs) {
        wordFreqs.forEach((word, frequency) -> {
            boolean isWordEnd = word.length() == prefix.length() + 1;
            if (!word.equals(prefix))
                freqs[word.charAt(prefix.length()) - 'A' + (isWordEnd ? Emission.NUM_LETTERS : 0)] += frequency;
        });
    }

    @Data
    @Builder
    private static class Candidate<State> {

        final List<String> words;
        final String currentPrefix;
        final double score;
        final State state;

        @Override
        public String toString() {
            return words + " " + currentPrefix + " " + score + " " + state;
        }
    }
}
