package com.kyc.snap.words;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import com.google.common.collect.ImmutableSortedMap;

public interface Dictionary {

    /** Returns all words in the dictionary in upper case. */
    SortedMap<String, Long> getWordFrequencies();

    Map<String, SortedMap<String, Long>> getBiWordFrequencies();

    default Set<String> getWords() {
        return getWordFrequencies().keySet();
    }

    default Map<String, Long> getWordFrequencies(String prefix) {
        return getWordFrequencies().subMap(prefix, prefix + Character.MAX_VALUE);
    }

    default Map<String, Long> getWordFrequencies(String prevWord, String prefix) {
        return getBiWordFrequencies()
                .getOrDefault(prevWord, ImmutableSortedMap.of())
                .subMap(prefix, prefix + Character.MAX_VALUE);
    }
}
