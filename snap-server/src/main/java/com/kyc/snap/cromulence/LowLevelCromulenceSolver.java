package com.kyc.snap.cromulence;

import java.util.ArrayList;
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

    private final DictionaryManager dictionary;
    private final Map<String, double[]> nextLetterProbabilitiesCache = new HashMap<>();

    public <State> CromulenceSolverResult solve(CromulenceSolverInput<State> input) {
        List<Candidate<State>> candidates = ImmutableList.of(Candidate.<State> builder()
            .words(ImmutableList.of())
            .currentPrefix("")
            .score(0)
            .state(input.initialState())
            .build());
        for (int n = 0; n < input.getNumEmissions(); n++) {
            List<Candidate<State>> newCandidates = new ArrayList<>();
            for (Candidate<State> candidate : candidates) {
                double[] nextLetterProbs = getNextLetterProbabilities(candidate.currentPrefix);
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
        Candidate<State> bestCandidate = candidates.stream()
                .filter(candidate -> candidate.currentPrefix.isEmpty())
                .findFirst()
                .get();
        return new CromulenceSolverResult(bestCandidate.words, bestCandidate.score);
    }

    private double[] getNextLetterProbabilities(String prefix) {
        if (nextLetterProbabilitiesCache.containsKey(prefix))
            return nextLetterProbabilitiesCache.get(prefix);
        double[] probs = new double[Emission.SIZE];
        dictionary.getWordFrequencies(prefix).forEach((word, frequency) -> {
            boolean isWordEnd = word.length() == prefix.length() + 1;
            if (!word.equals(prefix))
                probs[word.charAt(prefix.length()) - 'A' + (isWordEnd ? Emission.NUM_LETTERS : 0)] += frequency;
        });
        double totalProb = 0;
        for (double prob : probs)
            totalProb += prob;
        // TODO use n-grams for better probability estimation
        for (int i = 0; i < probs.length; i++)
            probs[i] /= totalProb;
        if (prefix.length() <= 2)
            nextLetterProbabilitiesCache.put(prefix, probs);
        return probs;
    }

    @Data
    @Builder
    private static class Candidate<State> {

        private final List<String> words;
        private final String currentPrefix;
        private final double score;
        private final State state;

        @Override
        public String toString() {
            return words + " " + currentPrefix + " " + score + " " + state;
        }
    }
}
