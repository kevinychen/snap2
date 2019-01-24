package com.kyc.snap.cromulence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.kyc.snap.words.DictionaryManager;

import lombok.Data;

public class CromulenceSolverTest {

    static DictionaryManager dictionary = new DictionaryManager();
    static NiceCromulenceSolver cromulence = new NiceCromulenceSolver(new CromulenceSolver(new LowLevelCromulenceSolver(dictionary)));

    @Test
    public void testSolveSlug() {
        CromulenceSolverResult result = cromulence.solveSlug("ICANFINDTHESPACESINTHISSLUG").get(0);
        assertThat(result.getWords()).containsExactly("I", "CAN", "FIND", "THE", "SPACES", "IN", "THIS", "SLUG");
    }

    @Test
    public void testSolveSlugWithWildcards() {
        CromulenceSolverResult result = cromulence.solveSlug("*H*S*nEH**so*E**KNeWN*E*tE*S").get(0);
        assertThat(result.getWords()).containsExactly("THIS", "ONE", "HAS", "SOME", "UNKNOWN", "LETTERS");
    }

    @Test
    public void testSolveSlugWithOptions() {
        CromulenceSolverResult result = cromulence.solveSlug("[AB][MN][RS][VW][DE][op]").get(0);
        assertThat(result.getWords()).containsExactly("ANSWER");
    }

    @Test
    public void testSolveSlugWithLengths() {
        CromulenceSolverResult result = cromulence.solveSlug("d*y**k****h***sw**", ImmutableList.of(2, 3, 4, 3, 6)).get(0);
        assertThat(result.getWords()).containsExactly("DO", "YOU", "KNOW", "THE", "ANSWER");
    }

    @Test
    public void testSolveAnagram() {
        CromulenceSolverResult result = cromulence.anagramSingleWord("CRROA*UL").get(0);
        assertThat(result.getWords()).containsExactly("ORACULAR");
    }

    @Test
    public void testSolveAnagramMultiword() {
        CromulenceSolverResult result = cromulence.anagramPhrase("AADDDEGILNNOORRRRUU").get(0);
        assertThat(result.getWords()).containsExactlyInAnyOrder("UNDERGROUND", "RAILROAD");
    }

    @Test
    public void testSolveRearrangement() {
        CromulenceSolverResult result = cromulence.solveRearrangement(ImmutableList.of("AS", "EL", "KE", "NE", "TA", "TH", "TO")).get(0);
        assertThat(result.getWords()).containsExactly("TAKE", "THE", "LAST", "ONE");
    }

    @Test
    public void testSolveRearrangementWithLengths() {
        CromulenceSolverResult result = cromulence.solveRearrangement(ImmutableList.of(
            "CHA", "DEB", "ERO", "GRA", "HES", "ITY", "LFO", "MPI", "ONC", "REV", "TIT", "TOF", "TOT", "UDE", "WEA", "WIL", "E"),
            ImmutableList.of(8, 4, 4, 7, 3, 1, 4, 2, 9, 2, 5)).get(0);
        assertThat(result.getWords()).containsExactly("CHAMPION", "CITY", "WILL", "FOREVER", "OWE", "A", "DEBT", "OF", "GRATITUDE", "TO",
            "THESE");
    }

    @Test
    public void testSolveRearrangementWithLengths2() {
        CromulenceSolverResult result = cromulence.solveRearrangement(ImmutableList.of(
            "ACE", "BLE", "CAL", "CHA", "DAY", "DWI", "EBI", "ERY", "GRA", "HEV", "HIS", "LLE", "LST", "NGE", "REF", "STH", "THT", "TOF",
            "VES", "WEA"),
            ImmutableList.of(2, 3, 5, 4, 3, 4, 7, 2, 10, 3, 5, 5, 4, 3)).get(0);
        assertThat(result.getWords()).containsExactly("WE", "ARE", "FACED", "WITH", "THE", "VERY", "GRAVEST", "OF", "CHALLENGES", "THE",
            "BIBLE", "CALLS", "THIS", "DAY");
    }

    @Data
    public static class TestState {

        private final int usedBitset;
        private final int currentPart;
        private final int currentIndex;
    }
}
