
package com.kyc.snap;

import org.junit.Test;

import com.kyc.snap.cromulence.CromulenceSolver;
import com.kyc.snap.cromulence.LowLevelCromulenceSolver;
import com.kyc.snap.cromulence.NiceCromulenceSolver;
import com.kyc.snap.wikinet.Wikinet;
import com.kyc.snap.words.DictionaryManager;

public class SnapTest {

    DictionaryManager dictionary = new DictionaryManager();
    NiceCromulenceSolver cromulence = new NiceCromulenceSolver(new CromulenceSolver(new LowLevelCromulenceSolver(dictionary)));
    Wikinet wikinet = new Wikinet();

    @Test
    public void test() throws Exception {
//        Files.lines(Paths.get("./test"))
//                .forEach(System.out::println);
//        Map<String, Long> wordFrequencies = dictionary.getWordFrequencies();
//        wordFrequencies.forEach((word, freq) -> {
//            if (freq < 10)
//                return;
//            System.out.println(word);
//        });
//        wikinet.getCleanedTitlesWithFrequencies().forEach(entry -> {
//            String word = entry.getKey().toUpperCase();
//            long freq = entry.getValue();
//            if (freq < 100)
//                return;
//            String[] split = word.split(" ");
//            if (Arrays.stream(split).map(String::length).collect(Collectors.toList()).equals(ImmutableList.of(1, 2, 3)))
//            if (wikinet.find(word).stream().anyMatch(a -> a.getSummary().contains("cat")))
//                System.out.println(word);
//        });
    }
}
