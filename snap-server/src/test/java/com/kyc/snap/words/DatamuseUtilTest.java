package com.kyc.snap.words;

import org.junit.Test;

import com.kyc.snap.words.DatamuseUtil.WordResult;

import static org.assertj.core.api.Assertions.assertThat;

public class DatamuseUtilTest {

    @Test
    public void testGetCommonWordsAfter() {
        assertThat(DatamuseUtil.getCommonWordsAfter("santa"))
                .extracting(WordResult::word)
                .contains("claus");
    }
}
