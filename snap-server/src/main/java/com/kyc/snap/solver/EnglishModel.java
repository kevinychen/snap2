package com.kyc.snap.solver;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kyc.snap.solver.GenericSolver.PriorModel;
import com.kyc.snap.words.EnglishDictionary;

public class EnglishModel implements PriorModel {

    private final EnglishDictionary dictionary = new EnglishDictionary();
    private final Cache<NextLetterFrequenciesKey, double[]> nextLetterFrequenciesCache = Caffeine.newBuilder()
            .maximumSize(100000)
            .build();
    private final Cache<String, double[]> singlePrefixFrequenciesCache = Caffeine.newBuilder()
            .maximumSize(100000)
            .build();

    private record NextLetterFrequenciesKey(String prevWord, String prefix) {}

    @Override
    public double[] getProbabilities(List<Integer> tokens) {
        int lastIndex = tokens.lastIndexOf(0);
        String prefix = toMessage(tokens.subList(lastIndex + 1, tokens.size()));
        String prevWord = lastIndex == -1
                ? null
                : toMessage(tokens.subList(tokens.subList(0, lastIndex).lastIndexOf(0) + 1, lastIndex));
        return nextLetterFrequenciesCache.get(new NextLetterFrequenciesKey(prevWord, prefix), key -> {
            double[] frequencies = Arrays.copyOf(getCachedFrequencies(prefix), EnglishTokens.NUM_LETTERS + 1);

            // bias toward words that appear in the bi-word list after the previous word
            if (prevWord != null)
                updateFrequencies(dictionary.getWordFrequencies(prevWord, prefix), prefix, frequencies);

            double totalProb = 0;
            for (double prob : frequencies)
                totalProb += prob;
            for (int i = 0; i < frequencies.length; i++)
                frequencies[i] /= totalProb;

            return frequencies;
        });
    }

    @Override
    public String toMessage(List<Integer> tokens) {
        StringBuilder b = new StringBuilder();
        for (int token : tokens)
            b.append(token == 0 ? ' ' : (char) (token + '@'));
        if (!b.isEmpty() && b.charAt(b.length() - 1) == ' ')
            b.setLength(b.length() - 1);
        return b.toString();
    }

    private double[] getCachedFrequencies(String prefix) {
        return singlePrefixFrequenciesCache.get(prefix, key -> {
            double[] frequencies = new double[EnglishTokens.NUM_LETTERS + 1];
            updateFrequencies(dictionary.getWordFrequencies(prefix), prefix, frequencies);
            return frequencies;
        });
    }

    private void updateFrequencies(Map<String, Long> wordFrequencies, String prefix, double[] frequencies) {
        wordFrequencies.forEach((word, frequency) -> {
            if (word.equals(prefix))
                frequencies[0] += frequency;
            else
                frequencies[word.charAt(prefix.length()) - '@'] += frequency;
        });
    }
}
