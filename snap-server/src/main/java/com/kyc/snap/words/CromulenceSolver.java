package com.kyc.snap.words;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import lombok.Data;

public class CromulenceSolver {

    private static final int MAX_WORD_LEN = 50;
    private static final int NUM_LETTERS = 26;
    private static final double IN_DICTIONARY_PROB = 0.96;

    private final DictionaryManager dictionary;
    private final double[] lenProbabilities = new double[MAX_WORD_LEN];
    private final Map<Parameters, double[]> nextLetterProbabilitiesCache = new HashMap<>();

    public CromulenceSolver(DictionaryManager dictionary) {
        this.dictionary = dictionary;

        for (int len = 1; len < MAX_WORD_LEN; len++)
            dictionary.getWordFrequencies(len, "").forEach((word, freq) -> {
                lenProbabilities[word.length()] += freq;
            });
        double totalProb = 0;
        for (double prob : lenProbabilities)
            totalProb += prob;
        for (int len = 1; len < MAX_WORD_LEN; len++)
            lenProbabilities[len] /= totalProb;
    }

    /**
     * @param word
     *            An upper case sequence of characters.
     * @return A score representing how likely the sequence of characters is an English word.
     */
    public double cromulence(String word) {
        double logProb = Math.log(lenProbabilities[word.length()]);
        String prefix = "";
        for (char c : word.toCharArray()) {
            logProb += Math.log(getNextLetterProbabilities(new Parameters(word.length(), prefix))[c - 'A']);
            prefix += c;
        }
        return logProb;
    }

    /**
     * @param words
     *            An upper case sequence of characters.
     * @return A score representing how likely the sequence of characters is a sequence of English
     *         words.
     */
    public Result bestCromulence(String words) {
        Result[] results = new Result[words.length() + 1];
        results[0] = new Result(new ArrayList<>(), 0);
        for (int i = 0; i < words.length(); i++) {
            for (int j = i + 1; j <= words.length(); j++) {
                String word = words.substring(i, j);
                double newScore = results[i].score + cromulence(word);
                if (results[j] == null || newScore > results[j].score) {
                    results[j] = new Result(ImmutableList.<String> builder()
                        .addAll(results[i].words)
                        .add(word)
                        .build(), newScore);
                }
            }
        }
        return results[words.length()];
    }

    public double[] getNextLetterProbabilities(Parameters parameters) {
        if (nextLetterProbabilitiesCache.containsKey(parameters))
            return nextLetterProbabilitiesCache.get(parameters);
        int prefixLen = parameters.getPrefix().length();
        double[] probs = new double[NUM_LETTERS];
        dictionary.getWordFrequencies(parameters.length, parameters.prefix).forEach((word, frequency) -> {
            probs[word.charAt(prefixLen) - 'A'] += frequency;
        });
        double totalProb = 1e-12;
        for (double prob : probs)
            totalProb += prob;
        // normalize so all probabilities add up to IN_DICTIONARY_PROB, and split remaining probability evenly
        for (int i = 0; i < NUM_LETTERS; i++)
            probs[i] = probs[i] / totalProb * IN_DICTIONARY_PROB + (1 - IN_DICTIONARY_PROB) / NUM_LETTERS;
        if (prefixLen <= 2)
            nextLetterProbabilitiesCache.put(parameters, probs);
        return probs;
    }

    @Data
    private static class Parameters {

        private final int length;
        private final String prefix;
    }

    @Data
    public static class Result {

        private final List<String> words;
        private final double score;
    }
}
