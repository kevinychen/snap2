package com.kyc.snap.words;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import lombok.Builder;
import lombok.Data;

@Data
public class CromulenceSolver {

    private static final int SEARCH_LIMIT = 1000;
    private static final int NUM_LETTERS = 26;

    private final DictionaryManager dictionary;
    private final Map<String, double[]> nextLetterProbabilitiesCache = new HashMap<>();

    public <State> Result solve(Input<State> input) {
        List<Candidate<State>> candidates = ImmutableList.of(Candidate.<State> builder()
            .words(ImmutableList.of())
            .currentPrefix("")
            .score(0)
            .state(input.initialState())
            .build());
        for (int n = 0; n < input.numEmissions; n++) {
            List<Candidate<State>> newCandidates = new ArrayList<>();
            for (Candidate<State> candidate : candidates) {
                double[] nextLetterProbs = getNextLetterProbabilities(candidate.currentPrefix);
                for (EmissionAndNewState<State> emission : input.getNextEmissions(candidate.state))
                    for (int j = 0; j < 2 * NUM_LETTERS; j++) {
                        double prob = nextLetterProbs[j] * emission.emission.probs[j];
                        if (prob > 0) {
                            Candidate.CandidateBuilder<State> newCandidate = candidate.toBuilder()
                                .score(candidate.score + Math.log(prob))
                                .state(emission.state);
                            if (j >= NUM_LETTERS) {
                                newCandidate
                                    .words(ImmutableList.<String> builder()
                                        .addAll(candidate.words)
                                        .add(candidate.currentPrefix + (char) ('A' + j % NUM_LETTERS))
                                        .build())
                                    .currentPrefix("");
                            } else {
                                newCandidate.currentPrefix(candidate.currentPrefix + (char) ('A' + j));
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
        return new Result(bestCandidate.words, bestCandidate.score);
    }

    private double[] getNextLetterProbabilities(String prefix) {
        if (nextLetterProbabilitiesCache.containsKey(prefix))
            return nextLetterProbabilitiesCache.get(prefix);
        double[] probs = new double[2 * NUM_LETTERS];
        dictionary.getWordFrequencies(prefix).forEach((word, frequency) -> {
            boolean isWordEnd = word.length() == prefix.length() + 1;
            if (!word.equals(prefix))
                probs[word.charAt(prefix.length()) - 'A' + (isWordEnd ? NUM_LETTERS : 0)] += frequency;
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
    public static abstract class Input<State> {

        private final int numEmissions;

        public abstract State initialState();

        public abstract List<EmissionAndNewState<State>> getNextEmissions(State state);
    }

    @Data
    public static class Emission {

        /**
         * An array with 52 entries, with probs[i] = probability of letter ('A' + i) for i≤25, and
         * probs[i] = probability of letter ('A' + i % 26) being the last letter of the word if
         * i≥26.
         */
        private final double[] probs;

        public static Emission of(char c) {
            double[] probs = new double[2 * NUM_LETTERS];
            probs[c - 'A'] = probs[c - 'A' + NUM_LETTERS] = 1.0;
            return new Emission(probs);
        }
    }

    @Data
    @Builder(toBuilder = true)
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

    @Data
    public static class EmissionAndNewState<State> {

        private final Emission emission;
        private final State state;
    }

    @Data
    public static class Result {

        private final List<String> words;
        private final double score;
    }
}
