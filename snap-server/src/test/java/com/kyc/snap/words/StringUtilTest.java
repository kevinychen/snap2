package com.kyc.snap.words;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StringUtilTest {

    @Test
    public void testCaesar() {
        assertThat(StringUtil.caesar("Pnrfne Fuvsg!", 13)).isEqualTo("Caesar Shift!");
    }

    @Test
    public void testAtbash() {
        assertThat(StringUtil.atbash("Zgyzhs Xrksvi!")).isEqualTo("Atbash Cipher!");
    }

    @Test
    public void testIsSubsequence() {
        assertThat(StringUtil.isSubsequence("", "")).isTrue();
        assertThat(StringUtil.isSubsequence("a", "")).isTrue();
        assertThat(StringUtil.isSubsequence("abc", "abc")).isTrue();
        assertThat(StringUtil.isSubsequence("snake", "sake")).isTrue();
        assertThat(StringUtil.isSubsequence("abracadabra", "aaaaa")).isTrue();

        assertThat(StringUtil.isSubsequence("", "a")).isFalse();
        assertThat(StringUtil.isSubsequence("a", "aa")).isFalse();
        assertThat(StringUtil.isSubsequence("abcabc", "cba")).isFalse();
        assertThat(StringUtil.isSubsequence("double", "oo")).isFalse();
    }

    @Test
    public void testSorted() {
        assertThat(StringUtil.sorted("")).isEqualTo("");
        assertThat(StringUtil.sorted("a")).isEqualTo("a");
        assertThat(StringUtil.sorted("abc")).isEqualTo("abc");
        assertThat(StringUtil.sorted("cdba")).isEqualTo("abcd");
        assertThat(StringUtil.sorted("abcba")).isEqualTo("aabbc");
        assertThat(StringUtil.sorted("aB")).isEqualTo("Ba");
    }

    @Test
    public void testRemove() {
        assertThat(StringUtil.remove("abc", 'a')).isEqualTo("bc");
        assertThat(StringUtil.remove("abc", 'b')).isEqualTo("ac");
        assertThat(StringUtil.remove("abc", 'c')).isEqualTo("ab");
        assertThat(StringUtil.remove("abcabc", 'c')).isEqualTo("ababc");
        assertThat(StringUtil.remove("abc", 'x')).isNull();
        assertThat(StringUtil.remove("abcabc", "cba")).isEqualTo("abc");
        assertThat(StringUtil.remove("abc", "x")).isNull();
    }

    @Test
    public void testRemoveSubseq() {
        assertThat(StringUtil.removeSubseq("abc", "ab")).isEqualTo("c");
        assertThat(StringUtil.removeSubseq("abcabc", "ca")).isEqualTo("abbc");
        assertThat(StringUtil.removeSubseq("abc", "ca")).isNull();
    }

    @Test
    public void testFindRearrangement() {
        assertThat(StringUtil.findRearrangement("", 0)).isEqualTo("");
        assertThat(StringUtil.findRearrangement("abc", 0)).isEqualTo("abc");
        assertThat(StringUtil.findRearrangement("abc", 1)).isEqualTo("acb");
        assertThat(StringUtil.findRearrangement("ACCCCEGIILMMOOOORSTWY", 2896836569686226L)).isEqualTo("CLOWESCOMICORMAGICTOY");
    }

    @Test
    public void testFindRearrangementIndex() {
        assertThat(StringUtil.findRearrangementIndex("")).isEqualTo(0);
        assertThat(StringUtil.findRearrangementIndex("abc")).isEqualTo(0);
        assertThat(StringUtil.findRearrangementIndex("acb")).isEqualTo(1);
        assertThat(StringUtil.findRearrangementIndex("MARES")).isEqualTo(50);
    }
}
