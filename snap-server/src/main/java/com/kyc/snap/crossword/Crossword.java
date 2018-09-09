package com.kyc.snap.crossword;

import java.util.List;

import lombok.Data;

@Data
public class Crossword {

    private final int numRows;
    private final int numCols;
    private final List<Entry> entries;

    @Data
    public static class Entry {

        private final int startRow;
        private final int startCol;
        private final int numSquares;
        private final Direction direction;

        private final String clueNumber;
        private final String clue;
    }

    public static enum Direction {
        ACROSS,
        DOWN,
    }
}
