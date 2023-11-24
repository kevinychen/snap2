package com.kyc.snap.crossword;

import java.util.List;

public record Crossword(int numRows, int numCols, List<Entry> entries) {

    public record Entry(int startRow, int startCol, int numSquares, ClueDirection direction, int clueNumber) {}
}
