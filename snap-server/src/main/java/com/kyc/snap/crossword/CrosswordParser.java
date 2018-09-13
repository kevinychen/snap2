package com.kyc.snap.crossword;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.kyc.snap.crossword.Crossword.Entry;
import com.kyc.snap.crossword.CrosswordClues.ClueSection;
import com.kyc.snap.crossword.CrosswordClues.NumberedClue;
import com.kyc.snap.grid.Grid;
import com.kyc.snap.grid.Grid.Square;
import com.kyc.snap.image.ImageUtils;

import lombok.Data;

public class CrosswordParser {

    private static final ClueDirection[] DIRECTIONS = { ClueDirection.ACROSS, ClueDirection.DOWN };

    public Crossword parseCrossword(Grid grid) {
        Square[][] squares = grid.getSquares();
        CrosswordSquare[][] crosswordSquares = new CrosswordSquare[grid.getNumRows()][grid.getNumCols()];
        for (int i = 0; i < grid.getNumRows(); i++)
            for (int j = 0; j < grid.getNumCols(); j++) {
                boolean isOpen = ImageUtils.isLight(squares[i][j].getRgb());
                boolean canGoAcross = isOpen && j < grid.getNumCols() - 1 && ImageUtils.isLight(squares[i][j + 1].getRgb());
                boolean canGoDown = isOpen && i < grid.getNumRows() - 1 && ImageUtils.isLight(squares[i + 1][j].getRgb());
                crosswordSquares[i][j] = new CrosswordSquare(isOpen, canGoAcross, canGoDown);
            }

        int clueNumber = 1;
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < grid.getNumRows(); i++)
            for (int j = 0; j < grid.getNumCols(); j++) {
                CrosswordSquare square = crosswordSquares[i][j];
                boolean hasEntry = false;
                if (square.canGoAcross && (j == 0 || !crosswordSquares[i][j - 1].canGoAcross)) {
                    int numSquares = 2;
                    while (crosswordSquares[i][j + numSquares - 1].canGoAcross)
                        numSquares++;
                    entries.add(new Entry(i, j, numSquares, ClueDirection.ACROSS, clueNumber));
                    hasEntry = true;
                }
                if (square.canGoDown && (i == 0 || !crosswordSquares[i - 1][j].canGoDown)) {
                    int numSquares = 2;
                    while (crosswordSquares[i + numSquares - 1][j].canGoDown)
                        numSquares++;
                    entries.add(new Entry(i, j, numSquares, ClueDirection.DOWN, clueNumber));
                    hasEntry = true;
                }
                if (hasEntry)
                    clueNumber++;
            }

        return new Crossword(grid.getNumRows(), grid.getNumCols(), entries);
    }

    public CrosswordClues parseClues(String text) {
        ClueDirection currentDirection = null;
        int currentClueNumber = -1;
        String currentClue = "";
        List<Clue> clues = new ArrayList<>();
        for (String line : text.split("\n")) {
            String normalizedLine = line.trim().toUpperCase();
            if (!currentClue.isEmpty() && (normalizedLine.startsWith("ACROSS") || normalizedLine.startsWith("DOWN")
                    || Character.isDigit(normalizedLine.charAt(0)))) {
                Preconditions.checkArgument(currentDirection != null, "No clue direction set");
                Preconditions.checkArgument(currentClueNumber != -1, "No clue number set");
                clues.add(new Clue(currentDirection, currentClueNumber, currentClue));
                currentClue = "";
            }

            if (normalizedLine.equalsIgnoreCase("ACROSS"))
                currentDirection = ClueDirection.ACROSS;
            else if (normalizedLine.equalsIgnoreCase("DOWN"))
                currentDirection = ClueDirection.DOWN;
            else {
                if (Character.isDigit(normalizedLine.charAt(0)))
                    currentClueNumber = Integer.parseInt(normalizedLine.split("\\D+")[0]);
                currentClue += line;
            }
        }
        clues.add(new Clue(currentDirection, currentClueNumber, currentClue));

        Multimap<ClueDirection, Clue> cluesByDirection = Multimaps.index(clues, clue -> clue.getDirection());
        List<ClueSection> sections = new ArrayList<>();
        for (ClueDirection direction : DIRECTIONS) {
            List<NumberedClue> sectionClues = cluesByDirection.get(direction).stream()
                .sorted(Comparator.comparing(clue -> clue.clueNumber))
                .map(clue -> new NumberedClue(clue.clueNumber, clue.clue))
                .collect(Collectors.toList());
            sections.add(new ClueSection(direction, sectionClues));
        }
        return new CrosswordClues(sections);
    }

    @Data
    private static class CrosswordSquare {

        private final boolean isOpen;
        private final boolean canGoAcross;
        private final boolean canGoDown;
    }

    @Data
    private static class Clue {

        private final ClueDirection direction;
        private final int clueNumber;
        private final String clue;
    }
}
