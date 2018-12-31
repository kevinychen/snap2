package com.kyc.snap.words;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.kyc.snap.words.CromulenceSolver.Result;

import lombok.Data;

public class CromulenceSolverTest {

    DictionaryManager dictionary = new DictionaryManager();
    CromulenceSolver cromulence = new CromulenceSolver(dictionary);

    @Test
    public void testSolveSlug() {
        Result result = cromulence.solveSlug("icanfindthespacesinthisslug");
        assertThat(result.getWords()).containsExactly("I", "CAN", "FIND", "THE", "SPACES", "IN", "THIS", "SLUG");
    }

    @Test
    public void testSolveSlugWithWildcards() {
        Result result = cromulence.solveSlug("*h*s*neh**so*e**k*ow**e*te*s");
        assertThat(result.getWords()).containsExactly("THIS", "ONE", "HAS", "SOME", "UNKNOWN", "LETTERS");
    }

    @Test
    public void testSolveRearrangement() {
        Result result = cromulence.solveRearrangement(ImmutableList.of("AD", "**", "AY", "BD", "DT", "TO", "WO", "YE"));
        assertThat(result.getWords()).containsExactlyInAnyOrder("ADD", "TWO", "TO", "BDAY", "YEAR");
    }

    @Data
    public static class TestState {

        private final int usedBitset;
        private final int currentPart;
        private final int currentIndex;
    }
}
