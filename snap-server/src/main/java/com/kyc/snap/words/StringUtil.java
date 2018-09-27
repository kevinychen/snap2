
package com.kyc.snap.words;

import java.util.Arrays;
import java.util.TreeSet;

import com.google.common.collect.Lists;
import com.kyc.snap.util.MathUtil;

public class StringUtil {

    /**
     * Caesar shifts the message.
     *
     * @param shift
     *            a nonnegative integer.
     */
    public static String caesar(String message, int shift) {
        StringBuilder sb = new StringBuilder();
        for (char c : message.toCharArray()) {
            char new_c = c;
            if (c >= 'A' && c <= 'Z')
                new_c = (char) ('A' + (c - 'A' + shift) % 26);
            else if (c >= 'a' && c <= 'z')
                new_c = (char) ('a' + (c - 'a' + shift) % 26);
            sb.append(new_c);
        }
        return sb.toString();
    }

    public static String atbash(String message) {
        StringBuilder sb = new StringBuilder();
        for (char c : message.toCharArray()) {
            char new_c = c;
            if (c >= 'A' && c <= 'Z')
                new_c = (char) ('A' + 'Z' - c);
            else if (c >= 'a' && c <= 'z')
                new_c = (char) ('a' + 'z' - c);
            sb.append(new_c);
        }
        return sb.toString();
    }

    public static boolean isSubsequence(String outer, String inner) {
        int index = 0;
        for (char c : inner.toCharArray()) {
            index = outer.indexOf(c, index) + 1;
            if (index == 0)
                return false;
        }
        return true;
    }

    public static String sorted(String message) {
        char[] chars = message.toCharArray();
        Arrays.sort(chars);
        return new String(chars);
    }

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

    private StringUtil() {
    }
}
