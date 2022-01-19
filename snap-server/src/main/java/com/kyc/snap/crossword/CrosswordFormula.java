package com.kyc.snap.crossword;

import lombok.Data;

@Data
public class CrosswordFormula {

    private final int row;
    private final int col;
    private final boolean isFormula;
    private final String value;
    private final Integer clueNumber;
}
