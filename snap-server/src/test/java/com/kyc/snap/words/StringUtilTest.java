package com.kyc.snap.words;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class StringUtilTest {

    @Test
    public void testRemove() {
        assertThat(StringUtil.remove("abc", 'a')).isEqualTo("bc");
        assertThat(StringUtil.remove("abc", 'b')).isEqualTo("ac");
        assertThat(StringUtil.remove("abc", 'c')).isEqualTo("ab");
        assertThat(StringUtil.remove("abcabc", 'c')).isEqualTo("ababc");
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
