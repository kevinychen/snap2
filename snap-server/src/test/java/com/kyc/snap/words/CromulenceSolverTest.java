package com.kyc.snap.words;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.kyc.snap.words.CromulenceSolver.Emission;
import com.kyc.snap.words.CromulenceSolver.EmissionAndNewState;
import com.kyc.snap.words.CromulenceSolver.Input;
import com.kyc.snap.words.CromulenceSolver.Result;

import lombok.Data;

public class CromulenceSolverTest {

    CromulenceSolver.Result best;

    @Test
    public void testBestCromulence() {
        DictionaryManager dictionary = new DictionaryManager();
        CromulenceSolver cromulence = new CromulenceSolver(dictionary);
        List<String> parts = ImmutableList.of("AD", "AR", "AY", "BD", "DT", "TO", "WO", "YE");
        Result result = cromulence.solve(new Input<TestState>(parts.stream().mapToInt(String::length).sum()) {

            @Override
            public TestState initialState() {
                return new TestState(0, -1, 0);
            }

            @Override
            public List<EmissionAndNewState<TestState>> getNextEmissions(TestState state) {
                List<EmissionAndNewState<TestState>> nextEmissions = new ArrayList<>();
                for (int i = 0; i < parts.size(); i++) {
                    if (state.currentPart != -1 && i != state.currentPart)
                        continue;
                    if ((state.usedBitset & (1 << i)) == 0) {
                        TestState newState = state.currentIndex == parts.get(i).length() - 1
                                ? new TestState(state.usedBitset | (1 << i), -1, 0)
                                : new TestState(state.usedBitset, i, state.currentIndex + 1);
                        nextEmissions.add(new EmissionAndNewState<>(
                                Emission.of(parts.get(i).charAt(state.currentIndex)),
                                newState));
                    }
                }
                return nextEmissions;
            }
        });
        assertThat(result.getWords()).containsExactlyInAnyOrder("ADD", "TWO", "TO", "BDAY", "YEAR");
    }

    @Data
    public static class TestState {

        private final int usedBitset;
        private final int currentPart;
        private final int currentIndex;
    }
}
