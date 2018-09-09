package com.kyc.snap.crossword;

import java.util.Map;

import lombok.Data;

@Data
public class CrosswordClues {

    private final Map<CluePosition, String> clues;

    @Data
    public static class CluePosition {

        private final ClueDirection direction;
        private final String clueNumber;
    }
}
