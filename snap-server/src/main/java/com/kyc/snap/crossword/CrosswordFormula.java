package com.kyc.snap.crossword;

public record CrosswordFormula(
        int row,
        int col,
        boolean formula,
        String value,
        Integer clueNumber) {}
