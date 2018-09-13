package com.kyc.snap.crossword;

import java.util.List;

import lombok.Data;

@Data
public class CrosswordClues {

    private final List<ClueSection> sections;

    @Data
    public static class ClueSection {

        private final ClueDirection direction;
        private final List<NumberedClue> clues;
    }

    @Data
    public static class NumberedClue {

        private final int clueNumber;
        private final String clue;
    }
}
