package com.kyc.snap.words;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class DictionaryManager {

    public static final String WORD_FREQUENCIES_FILE = "./data/count_1w.txt";

    private List<String> words;
    /**
     * { wordLength: { word: frequency } }
     */
    private Map<Integer, TreeMap<String, Long>> wordFrequencies;

    public DictionaryManager() {
        words = new ArrayList<>();
        wordFrequencies = new HashMap<>();
        try (Scanner scanner = new Scanner(new File(WORD_FREQUENCIES_FILE))) {
            while (scanner.hasNext()) {
                String word = scanner.next().toUpperCase();
                long freq = scanner.nextLong();
                words.add(word);
                wordFrequencies.computeIfAbsent(word.length(), len -> new TreeMap<>())
                    .put(word, freq);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getWords() {
        return words;
    }

    public Map<String, Long> getWordFrequencies(int wordLen, String prefix) {
        return wordFrequencies.getOrDefault(wordLen, new TreeMap<>())
            .subMap(prefix, prefix + Character.MAX_VALUE);
    }
}
