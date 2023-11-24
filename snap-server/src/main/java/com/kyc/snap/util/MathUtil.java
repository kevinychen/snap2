package com.kyc.snap.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Consumer;

public enum MathUtil {
    ;

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

    public static <T> void forEachPermutation(List<T> objects, Consumer<List<T>> f) {
        forEachPermutationHelper(new ArrayList<>(objects), new ArrayList<>(), f);
    }

    private static <T> void forEachPermutationHelper(List<T> objects, List<T> permutation, Consumer<List<T>> f) {
        if (objects.isEmpty()) {
            f.accept(permutation);
        }
        for (int i = 0; i < objects.size(); i++) {
            T obj = objects.remove(i);
            permutation.add(obj);
            forEachPermutationHelper(objects, permutation, f);
            objects.add(i, obj);
            permutation.remove(permutation.size() - 1);
        }
    }
}
