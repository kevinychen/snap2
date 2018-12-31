package com.kyc.snap.cromulence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;

import lombok.Data;

/**
 * Provides functions to solve standard cromulence problems, but still provides full flexibility on
 * probabilities of each letter.
 */
@Data
public class CromulenceSolver {

    private final LowLevelCromulenceSolver solver;

    public CromulenceSolverResult solveSlug(List<Emission> emissions) {
        return solver.solve(new CromulenceSolverInput<Integer>(emissions.size()) {

            @Override
            public Integer initialState() {
                return 0;
            }

            @Override
            public List<EmissionAndNewState<Integer>> getNextEmissions(Integer state) {
                return ImmutableList.of(new EmissionAndNewState<>(emissions.get(state), state + 1));
            }
        });
    }

    public CromulenceSolverResult solveRearrangement(List<List<Emission>> parts) {
        return solveRearrangement(parts, Collections.nCopies(parts.stream().mapToInt(List::size).sum(), 0.5));
    }

    public CromulenceSolverResult solveRearrangement(List<List<Emission>> parts, List<Double> endOfWordProbs) {
        return solver.solve(new CromulenceSolverInput<SolveRearrangementState>(parts.stream().mapToInt(List::size).sum()) {

            @Override
            public SolveRearrangementState initialState() {
                return new SolveRearrangementState(0, -1, 0);
            }

            @Override
            public List<EmissionAndNewState<SolveRearrangementState>> getNextEmissions(SolveRearrangementState state) {
                List<EmissionAndNewState<SolveRearrangementState>> nextEmissions = new ArrayList<>();
                int numLettersUsed = state.currentIndex;
                for (int i = 0; i < parts.size(); i++)
                    if ((state.usedBitset & (1 << i)) > 0)
                        numLettersUsed += parts.get(i).size();
                for (int i = 0; i < parts.size(); i++) {
                    if (state.currentPart != -1 && i != state.currentPart)
                        continue;
                    if ((state.usedBitset & (1 << i)) == 0) {
                        SolveRearrangementState newState = state.currentIndex == parts.get(i).size() - 1
                                ? new SolveRearrangementState(state.usedBitset | (1 << i), -1, 0)
                                : new SolveRearrangementState(state.usedBitset, i, state.currentIndex + 1);
                        nextEmissions.add(new EmissionAndNewState<>(
                                skewEmission(parts.get(i).get(state.currentIndex), endOfWordProbs.get(numLettersUsed)),
                                newState));
                    }
                }
                return nextEmissions;
            }
        });
    }

    private static Emission skewEmission(Emission emission, double endOfWordProb) {
        double[] newProbs = new double[Emission.SIZE];
        for (int i = 0; i < Emission.NUM_LETTERS; i++)
            newProbs[i] = emission.getProbs()[i] * 2 * (1 - endOfWordProb);
        for (int i = 0; i < Emission.NUM_LETTERS; i++)
            newProbs[i + Emission.NUM_LETTERS] = emission.getProbs()[i + Emission.NUM_LETTERS] * 2 * endOfWordProb;
        return new Emission(newProbs);
    }

    @Data
    private static class SolveRearrangementState {

        private final int usedBitset;
        private final int currentPart;
        private final int currentIndex;
    }
}
