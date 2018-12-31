package com.kyc.snap.words;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

public class DictionaryManager {

    public static final String WORD_FREQUENCIES_FILE = "./data/count_1w.txt";

    private TreeMap<String, Long> wordFrequencies;
    /**
     * { wordLength: { word: frequency } }
     */
    private Map<Integer, TreeMap<String, Long>> wordFrequenciesByLen;

    public DictionaryManager() {
        wordFrequencies = new TreeMap<>();
        wordFrequenciesByLen = new HashMap<>();
        try (Scanner scanner = new Scanner(new File(WORD_FREQUENCIES_FILE))) {
            while (scanner.hasNext()) {
                String word = scanner.next().toUpperCase();
                long freq = scanner.nextLong();
                wordFrequencies.put(word, freq);
                wordFrequenciesByLen.computeIfAbsent(word.length(), len -> new TreeMap<>())
                    .put(word, freq);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Set<String> getWords() {
        return wordFrequencies.keySet();
    }

    public Map<String, Long> getWordFrequencies(String prefix) {
        return wordFrequencies.subMap(prefix, prefix + Character.MAX_VALUE);
    }

    public Map<String, Long> getWordFrequencies(int wordLen, String prefix) {
        return wordFrequenciesByLen.getOrDefault(wordLen, new TreeMap<>())
            .subMap(prefix, prefix + Character.MAX_VALUE);
    }
}
