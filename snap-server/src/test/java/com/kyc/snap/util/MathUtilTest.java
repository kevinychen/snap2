package com.kyc.snap.util;

import java.math.BigInteger;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MathUtilTest {

    @Test
    public void testFactorial() {
        assertThat(MathUtil.factorial(0)).isEqualTo(BigInteger.valueOf(1));
        assertThat(MathUtil.factorial(1)).isEqualTo(BigInteger.valueOf(1));
        assertThat(MathUtil.factorial(2)).isEqualTo(BigInteger.valueOf(2));
        assertThat(MathUtil.factorial(3)).isEqualTo(BigInteger.valueOf(6));
        assertThat(MathUtil.factorial(10)).isEqualTo(BigInteger.valueOf(3628800));
    }

    @Test
    public void testNumRearrangements() {
        assertThat(MathUtil.numRearrangements(List.of())).isEqualTo(1);
        assertThat(MathUtil.numRearrangements(List.of(1))).isEqualTo(1);
        assertThat(MathUtil.numRearrangements(List.of(1, 2, 3))).isEqualTo(6);
        assertThat(MathUtil.numRearrangements(List.of(1, 2, 1, 2, 3))).isEqualTo(30);
    }
}
