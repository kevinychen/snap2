
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

    /**
     * Find the Levenshtein distance between a and b.
     * https://en.wikipedia.org/wiki/Levenshtein_distance
     */
    public static int levenshteinDistance(String a, String b) {
        int[] costs = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++)
            costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }

    private StringUtil() {
    }
}
