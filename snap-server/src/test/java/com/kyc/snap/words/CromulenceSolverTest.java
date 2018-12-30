package com.kyc.snap.words;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.kyc.snap.util.MathUtil;
import com.kyc.snap.words.CromulenceSolver.Result;

public class CromulenceSolverTest {

    CromulenceSolver.Result best;

    @Test
    public void testBestCromulence() {
        DictionaryManager dictionary = new DictionaryManager();
        CromulenceSolver cromulence = new CromulenceSolver(dictionary);
        List<String> parts = ImmutableList.of("AD", "AR", "AY", "BD", "DT", "TO", "WO", "YE");
        MathUtil.forEachPermutation(parts, perm -> {
            StringBuilder sb = new StringBuilder();
            for (String part : perm)
                sb.append(part);
            Result result = cromulence.bestCromulence(sb.toString());
            if (best == null || result.getScore() > best.getScore())
                best = result;
        });
        assertThat(best.getWords()).containsExactlyInAnyOrder("ADD", "TWO", "TO", "BDAY", "YEAR");
    }
}
