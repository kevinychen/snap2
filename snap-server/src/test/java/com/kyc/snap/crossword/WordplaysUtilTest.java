package com.kyc.snap.crossword;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import com.kyc.snap.crossword.WordplaysUtil.ClueSuggestion;

public class WordplaysUtilTest {

    @Test
    public void test() {
        List<ClueSuggestion> solutions = WordplaysUtil.fetchCrosswordClueSuggestions("rodent", 3);
        assertThat(solutions.get(0)).isEqualTo(new ClueSuggestion("RAT", 5));
    }
}
