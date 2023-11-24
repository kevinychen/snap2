
package com.kyc.snap;

import org.junit.Test;

import com.kyc.snap.solver.PregexSolver;
import com.kyc.snap.words.EnglishDictionary;
import com.kyc.snap.words.WikipediaTitlesDictionary;

public class SnapTest {

    EnglishDictionary dictionary = new EnglishDictionary();
    WikipediaTitlesDictionary wikipedia = new WikipediaTitlesDictionary();
    PregexSolver pregex = new PregexSolver();

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
//        wikipedia.getWordFrequencies().forEach((word, freq) -> {
//            String[] split = word.split(" ");
//            if (Arrays.stream(split).map(String::length).toList().equals(List.of(1, 2, 3)))
//                System.out.println(word);
//        });
    }
}
