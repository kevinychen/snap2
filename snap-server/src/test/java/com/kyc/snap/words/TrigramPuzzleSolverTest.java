package com.kyc.snap.words;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.google.common.base.Joiner;

public class TrigramPuzzleSolverTest {

    DictionaryManager dictionary = new DictionaryManager();
    TrigramPuzzleSolver trigramPuzzleSolver = new TrigramPuzzleSolver(dictionary);

    /*
     * From "What a Great Place To Start" in Puzzle Boat 4.
     */

    @Test
    public void test1() {
        testHelper(
            "CHA DEB ERO GRA HES ITY LFO MPI ONC REV TIT TOF TOT UDE WEA WIL E",
            "8 4 4 7 3 1 4 2 9 2 5",
            "CHAMPION CITY WILL FOREVER OWE A DEBT OF GRATITUDE TO THESE");
    }

    @Test
    public void test2() {
        testHelper(
            "ACE BLE CAL CHA DAY DWI EBI ERY GRA HEV HIS LLE LST NGE REF STH THT TOF VES WEA",
            "2 3 5 4 3 4 7 2 10 3 5 5 4 3",
            "WE ARE FACED WITH THE VERY GRAVEST OF CHALLENGES THE BIBLE CALLS THIS DAY");
    }

    private void testHelper(String trigramString, String wordLengthsString, String expectedString) {
        List<String> trigrams = Arrays.asList(trigramString.split(" "));
        List<Integer> wordLengths = Arrays.stream(wordLengthsString.split(" ")).map(Integer::parseInt).collect(Collectors.toList());
        List<String> solution = trigramPuzzleSolver.solve(trigrams, wordLengths);
        assertThat(Joiner.on(' ').join(solution)).isEqualTo(expectedString);
    }
}
