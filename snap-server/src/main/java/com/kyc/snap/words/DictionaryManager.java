package com.kyc.snap.words;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class DictionaryManager {

    public static final String WORD_FREQUENCIES_FILE = "./data/count_1w.txt";

    private TreeMap<String, Long> wordFrequencies;

    public DictionaryManager() {
        wordFrequencies = new TreeMap<>();
        try (Scanner scanner = new Scanner(new File(WORD_FREQUENCIES_FILE))) {
            while (scanner.hasNext())
                wordFrequencies.put(scanner.next().toUpperCase(), scanner.nextLong());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Long> getFrequenciesForWordsWithPrefix(String prefix) {
        return wordFrequencies.subMap(prefix, prefix + Character.MAX_VALUE);
    }
}
