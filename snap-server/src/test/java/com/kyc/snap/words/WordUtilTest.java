package com.kyc.snap.words;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class WordUtilTest {

    @Test
    public void testCaesar() {
        assertThat(WordUtil.caesar("Pnrfne Fuvsg!", 13)).isEqualTo("Caesar Shift!");
    }

    @Test
    public void testAtbash() {
        assertThat(WordUtil.atbash("Zgyzhs Xrksvi!")).isEqualTo("Atbash Cipher!");
    }

    @Test
    public void testIsSubsequence() {
        assertThat(WordUtil.isSubsequence("", "")).isTrue();
        assertThat(WordUtil.isSubsequence("a", "")).isTrue();
        assertThat(WordUtil.isSubsequence("abc", "abc")).isTrue();
        assertThat(WordUtil.isSubsequence("snake", "sake")).isTrue();
        assertThat(WordUtil.isSubsequence("abracadabra", "aaaaa")).isTrue();

        assertThat(WordUtil.isSubsequence("", "a")).isFalse();
        assertThat(WordUtil.isSubsequence("a", "aa")).isFalse();
        assertThat(WordUtil.isSubsequence("abcabc", "cba")).isFalse();
        assertThat(WordUtil.isSubsequence("double", "oo")).isFalse();
    }

    @Test
    public void testSorted() {
        assertThat(WordUtil.sorted("")).isEqualTo("");
        assertThat(WordUtil.sorted("a")).isEqualTo("a");
        assertThat(WordUtil.sorted("abc")).isEqualTo("abc");
        assertThat(WordUtil.sorted("cdba")).isEqualTo("abcd");
        assertThat(WordUtil.sorted("abcba")).isEqualTo("aabbc");
        assertThat(WordUtil.sorted("aB")).isEqualTo("Ba");
    }
}
