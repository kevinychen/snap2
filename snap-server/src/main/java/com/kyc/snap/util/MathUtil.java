package com.kyc.snap.util;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class MathUtil {

    public static BigInteger factorial(int n) {
        BigInteger factorial = BigInteger.ONE;
        for (int i = 1; i <= n; i++)
            factorial = factorial.multiply(BigInteger.valueOf(i));
        return factorial;
    }

    /**
     * Returns the number of distinct rearrangements of the given values.
     */
    public static <T> long numRearrangements(List<T> values) {
        BigInteger numRearrangements = factorial(values.size());
        for (T value : new HashSet<>(values))
            numRearrangements = numRearrangements.divide(factorial(Collections.frequency(values, value)));
        return numRearrangements.longValue();
    }

    private MathUtil() {}
}
