package com.kyc.snap.words;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PhoneticsUtilTest {

    @Test
    public void testPhoneticDifference() {
        assertThat(PhoneticsUtil.difference("B", "B")).isEqualTo(0);
        assertThat(PhoneticsUtil.difference("", "UH")).isEqualTo(0);
        assertThat(PhoneticsUtil.difference("EY", "EH")).isEqualTo(0);

        assertThat(PhoneticsUtil.difference("B", "P")).isEqualTo(1);
        assertThat(PhoneticsUtil.difference("B", "V")).isEqualTo(1);
        assertThat(PhoneticsUtil.difference("CH", "JH")).isEqualTo(1);
        assertThat(PhoneticsUtil.difference("G", "K")).isEqualTo(1);
        assertThat(PhoneticsUtil.difference("T", "TH")).isEqualTo(1);
        assertThat(PhoneticsUtil.difference("D", "DH")).isEqualTo(1);
        assertThat(PhoneticsUtil.difference("AE", "EY")).isEqualTo(1);
        assertThat(PhoneticsUtil.difference("IY", "EH")).isEqualTo(1);
        assertThat(PhoneticsUtil.difference("AA", "AO")).isEqualTo(1);

        assertThat(PhoneticsUtil.difference("B", "F")).isEqualTo(2);
        assertThat(PhoneticsUtil.difference("S", "SH")).isEqualTo(2);
        assertThat(PhoneticsUtil.difference("N", "NG")).isEqualTo(2);
        assertThat(PhoneticsUtil.difference("L", "R")).isEqualTo(2);
        assertThat(PhoneticsUtil.difference("AA", "AE")).isEqualTo(2);
        assertThat(PhoneticsUtil.difference("", "IH")).isEqualTo(2);

        assertThat(PhoneticsUtil.difference("B", "L")).isGreaterThan(5);
        assertThat(PhoneticsUtil.difference("D", "")).isGreaterThan(5);
        assertThat(PhoneticsUtil.difference("S", "AA")).isGreaterThan(5);
    }
}
