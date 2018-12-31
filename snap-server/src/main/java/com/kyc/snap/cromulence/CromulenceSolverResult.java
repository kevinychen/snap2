package com.kyc.snap.cromulence;

import java.util.List;

import lombok.Data;

@Data
public class CromulenceSolverResult {

    private final List<String> words;
    private final double score;
}
