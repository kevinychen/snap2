package com.kyc.snap.cromulence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.kyc.snap.words.DictionaryManager;

import lombok.Data;

public class CromulenceSolverTest {

    DictionaryManager dictionary = new DictionaryManager();
    NiceCromulenceSolver cromulence = new NiceCromulenceSolver(new CromulenceSolver(new LowLevelCromulenceSolver(dictionary)));

    @Test
    public void testSolveSlug() {
        CromulenceSolverResult result = cromulence.solveSlug("ICANFINDTHESPACESINTHISSLUG");
        assertThat(result.getWords()).containsExactly("I", "CAN", "FIND", "THE", "SPACES", "IN", "THIS", "SLUG");
    }

    @Test
    public void testSolveSlugWithWildcards() {
        CromulenceSolverResult result = cromulence.solveSlug("*H*S*nEH**so*E**KNeWN*E*tE*S");
        assertThat(result.getWords()).containsExactly("THIS", "ONE", "HAS", "SOME", "UNKNOWN", "LETTERS");
    }

    @Test
    public void testSolveRearrangement() {
        CromulenceSolverResult result = cromulence.solveRearrangement(ImmutableList.of("AD", "**", "AY", "BD", "DT", "TO", "WO", "YE"));
        assertThat(result.getWords()).containsExactlyInAnyOrder("ADD", "TWO", "TO", "BDAY", "YEAR");
    }

    @Data
    public static class TestState {

        private final int usedBitset;
        private final int currentPart;
        private final int currentIndex;
    }
}
