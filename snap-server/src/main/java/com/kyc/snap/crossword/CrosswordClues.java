package com.kyc.snap.crossword;

import java.util.List;

import lombok.Data;

@Data
public class CrosswordClues {

    private final List<Clue> clues;

    @Data
    public static class Clue {

        private final ClueDirection direction;
        private final int clueNumber;
        private final String clue;
    }
}
