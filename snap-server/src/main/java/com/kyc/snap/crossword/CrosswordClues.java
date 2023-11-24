package com.kyc.snap.crossword;

import java.util.List;

public record CrosswordClues(List<ClueSection> sections) {

    public record ClueSection(ClueDirection direction, List<NumberedClue> clues) {}

    public record NumberedClue(int clueNumber, String clue) {}
}
