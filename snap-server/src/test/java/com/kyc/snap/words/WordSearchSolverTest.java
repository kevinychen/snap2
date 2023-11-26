package com.kyc.snap.words;

import java.util.List;

import org.junit.Test;

import com.kyc.snap.words.WordSearchSolver.Result;

import static org.assertj.core.api.Assertions.assertThat;

public class WordSearchSolverTest {

    static final EnglishDictionary dictionary = new EnglishDictionary();
    static final WordSearchSolver solver = new WordSearchSolver();

    @Test
    public void findStraight() {
        assertThat(solver.find(List.of("ABC", "DEF", "GHI"), dictionary, false, List.of(3)).results())
                .extracting(Result::word)
                .contains("FED")
                .doesNotContain("BEG");
    }

    @Test
    public void findBoggle() {
        assertThat(solver.find(List.of("ABC", "DEF", "GHI"), dictionary, true, List.of(3)).results())
                .extracting(Result::word)
                .contains("FED", "BEG");
    }

    @Test
    public void findFuzzy() {
        assertThat(solver.find(List.of("AFB", "CUD", "EXF", "GZH", "IYJ"), dictionary, false, List.of(3, 5)).results())
                .extracting(Result::word)
                .contains("FUZZY");
    }

    @Test
    public void findNotAlpha() {
        assertThat(solver.find(List.of("ALPHA#"), dictionary, false, List.of(5)).results())
                .extracting(Result::word)
                .contains("ALPHA");
    }
}
