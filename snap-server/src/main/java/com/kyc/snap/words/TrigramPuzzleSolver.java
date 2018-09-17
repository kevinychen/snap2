
package com.kyc.snap.words;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Data;

@Data
public class TrigramPuzzleSolver {

    private static final int TRIGRAM_LENGTH = 3;
    private static final int MAX_BUFFER_SIZE = 1000;
    private static final int NUM_LETTERS = 26;
    private static final double IN_DICTIONARY_PROB = 0.9;

    private final DictionaryManager dictionary;

    public List<String> solve(List<String> trigrams, List<Integer> wordLengths) {
        // wordTemplates[i] is the word that letter i is contained in
        List<WordTemplate> wordTemplates = new ArrayList<>();
        for (int wordLength : wordLengths) {
            WordTemplate wordTemplate = new WordTemplate(wordTemplates.size(), wordLength);
            for (int i = 0; i < wordLength; i++)
                wordTemplates.add(wordTemplate);
        }

        List<MessageCandidate> candidates = new ArrayList<>();
        candidates.add(new MessageCandidate("", 1, 0));
        for (int n = 0; n < trigrams.size(); n++) {
            List<MessageCandidate> newCandidates = new ArrayList<>();
            for (MessageCandidate candidate : candidates)
                for (int i = 0; i < trigrams.size(); i++)
                    if ((candidate.usedBitset & (1 << i)) == 0) {
                        String trigram = trigrams.get(i);
                        // pieces that contain less than 3 pieces must go at the end
                        if ((trigram.length() != TRIGRAM_LENGTH) && (Integer.bitCount(candidate.usedBitset) < trigrams.size() - 1))
                            continue;
                        String newMessage = candidate.message;
                        double newProbability = candidate.probability;
                        for (char nextLetter : trigram.toCharArray()) {
                            newProbability *= nextLetterProbability(newMessage, nextLetter, wordTemplates);
                            newMessage += nextLetter;
                        }
                        newCandidates.add(new MessageCandidate(newMessage, newProbability, candidate.usedBitset | (1 << i)));
                    }
            candidates = newCandidates.stream()
                    .sorted(Comparator.comparing(candidate -> -candidate.probability))
                    .limit(MAX_BUFFER_SIZE)
                    .collect(Collectors.toList());
        }

        int index = 0;
        String bestMessage = candidates.get(0).message;
        List<String> solution = new ArrayList<>();
        for (int wordLength : wordLengths) {
            solution.add(bestMessage.substring(index, index + wordLength));
            index += wordLength;
        }
        return solution;
    }

    private Map<PrefixCacheKey, double[]> nextLetterCountsForPrefixCache = new HashMap<>();

    private double nextLetterProbability(String message, char nextLetter, List<WordTemplate> wordTemplates) {
        WordTemplate wordTemplate = wordTemplates.get(message.length());
        String prefix = message.substring(wordTemplate.start);
        PrefixCacheKey key = new PrefixCacheKey(prefix, wordTemplate.length);
        double[] counts;
        if (nextLetterCountsForPrefixCache.containsKey(key))
            counts = nextLetterCountsForPrefixCache.get(key);
        else {
            counts = new double[NUM_LETTERS];
            dictionary.getFrequenciesForWordsWithPrefix(prefix).forEach((word, frequency) -> {
                if (word.length() == wordTemplate.length)
                    // taking the square root gives a more accurate estimation of relative probabilities
                    counts[word.charAt(prefix.length()) - 'A'] += Math.sqrt(frequency);
            });
            if (prefix.length() <= 2)
                nextLetterCountsForPrefixCache.put(key, counts);
        }
        double totalCount = 0;
        for (double count : counts)
            totalCount += count;
        if (totalCount == 0)
            return 1. / NUM_LETTERS;
        else
            return IN_DICTIONARY_PROB * counts[nextLetter - 'A'] / totalCount + (1 - IN_DICTIONARY_PROB) / NUM_LETTERS;
    }

    @Data
    private static class WordTemplate {

        private final int start;
        private final int length;
    }

    @Data
    private static class MessageCandidate {

        private final String message;
        private final double probability;
        private final int usedBitset;
    }

    @Data
    private static class PrefixCacheKey {

        private final String prefix;
        private final int length;
    }
}
