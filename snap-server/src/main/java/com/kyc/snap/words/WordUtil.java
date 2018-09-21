package com.kyc.snap.words;

import java.util.Arrays;

public class WordUtil {

    /**
     * Caesar shifts the message.
     * @param shift a nonnegative integer.
     * @return
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

    private WordUtil() {}
}
