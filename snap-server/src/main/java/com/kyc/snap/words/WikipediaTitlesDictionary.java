package com.kyc.snap.words;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.TreeMap;

public class WikipediaTitlesDictionary implements Dictionary {

    public static final String FILE = "./data/wikipedia-titles";

    private final SortedMap<String, Long> wordFrequencies = new TreeMap<>();

    @Override
    public SortedMap<String, Long> getWordFrequencies() {
        if (wordFrequencies.isEmpty()) {
            try (Scanner scanner = new Scanner(new File(FILE))) {
                while (scanner.hasNext())
                    wordFrequencies.put(scanner.next().toUpperCase().replaceAll("[^A-Z]+", " ").trim(), 1L);
            } catch(FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return wordFrequencies;
    }

    @Override
    public Map<String, SortedMap<String, Long>> getBiWordFrequencies() {
        return Map.of();
    }
}
