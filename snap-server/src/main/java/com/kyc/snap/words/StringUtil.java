package com.kyc.snap.words;

import java.util.TreeSet;

import com.google.common.collect.Lists;
import com.kyc.snap.util.MathUtil;

public class StringUtil {

    /**
     * Returns the string with the specified character removed. If the string contains multiple
     * occurrences of the character, only the first occurrence is removed.
     */
    public static String remove(String string, char toRemove) {
        int index = string.indexOf(toRemove);
        return string.substring(0, index) + string.substring(index + 1);
    }

    /**
     * If all distinct rearrangements of the specified letters are arranged in alphabetical order,
     * returns the rearrangement at the specified index (0-indexed).
     */
    public static String findRearrangement(String letters, long index) {
        if (letters.isEmpty() && index == 0)
            return "";
        for (char c : new TreeSet<>(Lists.charactersOf(letters))) {
            String remaining = remove(letters, c);
            long numRearrangements = MathUtil.numRearrangements(Lists.charactersOf(remaining));
            if (index >= numRearrangements)
                index -= numRearrangements;
            else
                return c + findRearrangement(remaining, index);
        }
        throw new IllegalArgumentException("index too large");
    }

    /**
     * If all distinct rearrangements of the specified letters are arranged in alphabetical order,
     * returns the index at which the given rearrangement would appear (0-indexed).
     */
    public static long findRearrangementIndex(String letters) {
        long low = 0, high = MathUtil.numRearrangements(Lists.charactersOf(letters));
        while (low + 1 < high) {
            long mid = (low + high) / 2;
            if (findRearrangement(letters, mid).compareTo(letters) <= 0)
                low = mid;
            else
                high = mid;
        }
        return low;
    }

    private StringUtil() {}
}
