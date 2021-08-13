package com.kyc.snap.cromulence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.Test;

import com.kyc.snap.words.DictionaryManager;

public class CromulenceSolverTest {

    static DictionaryManager dictionary = new DictionaryManager();
    CromulenceSolver cromulence = new CromulenceSolver(dictionary);

    @Test
    public void testSolve() {
        CromulenceSolverResult result = cromulence.solve("ICANFINDTHESPACESINTHISSLUG", null).get(0);
        assertThat(result.getWords()).containsExactly("I", "CAN", "FIND", "THE", "SPACES", "IN", "THIS", "SLUG");
    }

    @Test
    public void testSolveWithWildcards() {
        CromulenceSolverResult result = cromulence.solve(".H.S.nEH..so.E..KNeWN.E.tE.S", null).get(0);
        assertThat(result.getWords()).containsExactly("THIS", "ONE", "HAS", "SOME", "UNKNOWN", "LETTERS");
    }

    @Test
    public void testSolveWithChoices() {
        CromulenceSolverResult result = cromulence.solve("[AB][MN][RS][VW][DE][op]", null).get(0);
        assertThat(result.getWords()).containsExactly("ANSWER");
    }

    @Test
    public void testSolveWithLengths() {
        CromulenceSolverResult result = cromulence.solve("d.y..k....h...sw..", List.of(2, 3, 4, 3, 6)).get(0);
        assertThat(result.getWords()).containsExactly("DO", "YOU", "KNOW", "THE", "ANSWER");
    }

    @Test
    public void testSolveAnagram() {
        CromulenceSolverResult result = cromulence.solve("\"<CRROA.UL>\"", null).get(0);
        assertThat(result.getWords()).containsExactly("ORACULAR");
    }

    @Test
    public void testSolveAnagramMultiword() {
        CromulenceSolverResult result = cromulence.solve("<AADDDEGILNNOORRRRUU>", null).get(0);
        assertThat(result.getWords()).containsExactlyInAnyOrder("UNDERGROUND", "RAILROAD");
    }

    @Test
    public void testSolveRearrangement() {
        cromulence.solve("<(AS)(EL)(KE)(NE)(TA)(TH)(TO)>", null)
            .forEach(System.out::println);
        CromulenceSolverResult result = cromulence.solve("<(AS)(EL)(KE)(NE)(TA)(TH)(TO)>", null).get(0);
        assertThat(result.getWords()).containsExactly("TAKE", "THE", "LAST", "ONE");
    }

    @Test
    public void testSolveRearrangementWithLengths() {
        CromulenceSolverResult result =
            cromulence.solve("<(CHA)(DEB)(ERO)(GRA)(HES)(ITY)(LFO)(MPI)(ONC)(REV)(TIT)(TOF)(TOT)(UDE)(WEA)(WIL)(E)>",
            List.of(8, 4, 4, 7, 3, 1, 4, 2, 9, 2, 5)).get(0);
        assertThat(result.getWords()).containsExactly("CHAMPION", "CITY", "WILL", "FOREVER", "OWE", "A", "DEBT", "OF", "GRATITUDE", "TO",
            "THESE");
    }

    @Test
    public void testSolveRearrangementWithLengths2() {
        CromulenceSolverResult result = cromulence.solve(
            "<(ACE)(BLE)(CAL)(CHA)(DAY)(DWI)(EBI)(ERY)(GRA)(HEV)(HIS)(LLE)(LST)(NGE)(REF)(STH)(THT)(TOF)(VES)(WEA)>",
            List.of(2, 3, 5, 4, 3, 4, 7, 2, 10, 3, 5, 5, 4, 3)).get(0);
        assertThat(result.getWords()).containsExactly("WE", "ARE", "FACED", "WITH", "THE", "VERY", "GRAVEST", "OF", "CHALLENGES", "THE",
            "BIBLE", "CALLS", "THIS", "DAY");
    }
}
