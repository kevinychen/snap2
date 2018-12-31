package com.kyc.snap.cromulence;

import lombok.Data;

@Data
public class EmissionAndNewState<State> {

    private final Emission emission;
    private final State state;
}
