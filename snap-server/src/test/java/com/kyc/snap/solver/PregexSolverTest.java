package com.kyc.snap.solver;

import java.util.List;

import org.junit.Test;

import com.kyc.snap.solver.GenericSolver.Result;
import com.kyc.snap.words.EnglishDictionary;

import static org.assertj.core.api.Assertions.assertThat;

public class PregexSolverTest {

    static final PregexSolver solver = new PregexSolver(new EnglishModel(new EnglishDictionary()));

    @Test
    public void slug() {
        Result result = solver.solve("ICANFINDTHESPACESINTHISSLUG", null).get(0);
        assertThat(result.message()).isEqualTo("I CAN FIND THE SPACES IN THIS SLUG");
    }

    @Test
    public void wildcards() {
        Result result = solver.solve(".H.S.nEH..so.E..KNeWN.E.tE.S", null).get(0);
        assertThat(result.message()).isEqualTo("THIS ONE HAS SOME UNKNOWN LETTERS");
    }

    @Test
    public void choices() {
        Result result = solver.solve("[AB][MN][RS][VW][DE][op]", null).get(0);
        assertThat(result.message()).isEqualTo("ANSWER");
    }

    @Test
    public void wordLengths() {
        Result result = solver.solve("d.y..k....h...sw..", List.of(2, 3, 4, 3, 6)).get(0);
        assertThat(result.message()).isEqualTo("DO YOU KNOW THE ANSWER");
    }

    @Test
    public void withLengthsAndFlexibleStart() {
        Result result = solver.solve(".....MENON", List.of(10)).get(0);
        assertThat(result.message()).isEqualTo("PHENOMENON");
    }

    @Test
    public void anagram() {
        Result result = solver.solve("\"<CRROA.UL>\"", null).get(0);
        assertThat(result.message()).isEqualTo("ORACULAR");
    }

    @Test
    public void anagramMultipleWords() {
        Result result = solver.solve("<AADDDEGILNNOORRRRUU>", null).get(0);
        assertThat(result.message().split(" ")).containsExactlyInAnyOrder("UNDERGROUND", "RAILROAD");
    }

    @Test
    public void rearrangement() {
        Result result = solver.solve("<(AS)(EL)(KE)(NE)(TA)(TH)(TO)>", null).get(0);
        assertThat(result.message()).isEqualTo("TAKE THE LAST ONE");
    }

    @Test
    public void rearrangementWithWordLengths() {
        Result result = solver.solve(
                "<(CHA)(DEB)(ERO)(GRA)(HES)(ITY)(LFO)(MPI)(ONC)(REV)(TIT)(TOF)(TOT)(UDE)(WEA)(WIL)(E)>",
                List.of(8, 4, 4, 7, 3, 1, 4, 2, 9, 2, 5)).get(0);
        assertThat(result.message()).isEqualTo("CHAMPION CITY WILL FOREVER OWE A DEBT OF GRATITUDE TO THESE");
    }

    @Test
    public void rearrangementWithWordLengths2() {
        Result result = solver.solve(
                "<(ACE)(BLE)(CAL)(CHA)(DAY)(DWI)(EBI)(ERY)(GRA)(HEV)(HIS)(LLE)(LST)(NGE)(REF)(STH)(THT)(TOF)(VES)(WEA)>",
                List.of(2, 3, 5, 4, 3, 4, 7, 2, 10, 3, 5, 5, 4, 3)).get(0);
        assertThat(result.message())
                .isEqualTo("WE ARE FACED WITH THE VERY GRAVEST OF CHALLENGES THE BIBLE CALLS THIS DAY");
    }

    @Test
    public void rearrangementWithIncompleteLengths() {
        Result result = solver.solve(
                "<(AR)(EL)(EM)(EN)(EN)(ET)(GT)(HI)(HS)(IT)(NC)(NG)(OM)(PL)(RA)(RE)(TW)>",
                List.of(13, 4)).get(0);
        assertThat(result.message()).isEqualTo("REARRANGEMENT WITH");
    }

    @Test
    public void rearrangementWithQuestionMarks() {
        Result result = solver.solve("<(MEN)?(NGE)?(NMA)?(REA)?(RKS)?(RRA)?(THQ)?(TIO)?(TWI)?(UES)?>", null).get(0);
        assertThat(result.message()).isEqualTo("REARRANGEMENT WITH QUESTION MARKS");
    }

    @Test
    public void multipleAnagrams() {
        Result result = solver.solve("<HET><RAL><SEG><TAN><RUT><BLA><OODY><AFL><NDI><CIN><AWE><TER>", null).get(0);
        assertThat(result.message()).isEqualTo("THE LARGEST NATURAL BODY OF LAND IN ICE WATER");
    }

    @Test
    public void interleave() {
        Result result = solver.solve("(TREVE~INELA)", null).get(0);
        assertThat(result.message()).isEqualTo("INTERLEAVE");
    }

    @Test
    public void and() {
        Result result = solver.solve("(A[NOP].)&(.[MNO]D)", null).get(0);
        assertThat(result.message()).isEqualTo("AND");
    }

    @Test
    public void wordBoundary() {
        Result result = solver.solve("BOUND\\bARY", null).get(0);
        assertThat(result.message()).isEqualTo("BOUND AR Y");
    }

    @Test
    public void chain() {
        Result result = solver.solve("\\chain((TE)(..)(LI)<DR>(LO)(..)(NA)))", null).get(0);
        assertThat(result.message().split(" ")).containsExactlyInAnyOrder("LORD", "LIEUTENANT");
    }

    @Test
    public void noDuplicates() {
        assertThat(solver.solve("[BC][AA]T", null))
                .extracting(Result::message)
                .contains("BAT", "CAT")
                .doesNotHaveDuplicates();
    }
}