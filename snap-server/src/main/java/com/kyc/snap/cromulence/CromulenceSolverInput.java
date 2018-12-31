package com.kyc.snap.cromulence;

import java.util.List;

import lombok.Data;

@Data
public abstract class CromulenceSolverInput<State> {

    private final int numEmissions;

    public abstract State initialState();

    public abstract List<EmissionAndNewState<State>> getNextEmissions(State state);
}
