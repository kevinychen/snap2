package com.kyc.snap.words;

import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class CustomDictionary implements Dictionary {

    private final SortedMap<String, Long> wordFrequencies = new TreeMap<>();

    public CustomDictionary(Collection<String> words) {
        for (String word : words)
            wordFrequencies.put(word.toUpperCase().replaceAll("[^A-Z]+", ""), 1L);
    }

    @Override
    public SortedMap<String, Long> getWordFrequencies() {
        return wordFrequencies;
    }

    @Override
    public Map<String, SortedMap<String, Long>> getBiWordFrequencies() {
        return Map.of();
    }
}
