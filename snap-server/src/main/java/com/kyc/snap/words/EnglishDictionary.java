package com.kyc.snap.words;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.TreeMap;

public class EnglishDictionary implements Dictionary {

    public static final String WORD_FREQUENCIES_FILE = "./data/count_1w.txt";
    public static final String BIWORD_FREQUENCIES_FILE = "./data/count_2w.txt";
    /**
     * The value to scale frequencies in the biword frequencies file. This value was estimated
     * heuristically such that all biword frequencies (which are greater than 100,000) automatically
     * map to frequencies in the top 1 percentile of single word frequencies.
     */
    public static final int BIWORD_FREQUENCY_MULTIPLIER = 10000;

    private final SortedMap<String, Long> wordFrequencies = new TreeMap<>();
    private final Map<String, SortedMap<String, Long>> biwordFrequencies = new HashMap<>();

    public EnglishDictionary() {
        try (Scanner scanner = new Scanner(new File(WORD_FREQUENCIES_FILE));
                Scanner scanner2 = new Scanner(new File(BIWORD_FREQUENCIES_FILE))) {
            while (scanner.hasNext())
                wordFrequencies.put(scanner.next().toUpperCase(), scanner.nextLong());
            while (scanner2.hasNext()) {
                biwordFrequencies.computeIfAbsent(scanner2.next().toUpperCase(), key -> new TreeMap<>())
                    .put(scanner2.next().toUpperCase(), scanner2.nextLong() * BIWORD_FREQUENCY_MULTIPLIER);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SortedMap<String, Long> getWordFrequencies() {
        return wordFrequencies;
    }

    public Map<String, SortedMap<String, Long>> getBiWordFrequencies() {
        return biwordFrequencies;
    }
}
