package com.kyc.snap.crossword;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.kyc.snap.crossword.Crossword.Entry;
import com.kyc.snap.crossword.CrosswordClues.ClueSection;
import com.kyc.snap.crossword.CrosswordClues.NumberedClue;
import com.kyc.snap.grid.Border.Style;
import com.kyc.snap.grid.Grid;
import com.kyc.snap.grid.Grid.Square;
import com.kyc.snap.image.ImageUtils;

import lombok.Data;

public class CrosswordParser {

    private static final ClueDirection[] DIRECTIONS = { ClueDirection.ACROSS, ClueDirection.DOWN, ClueDirection.UNKNOWN };

    public Crossword parseCrossword(Grid grid) {
        Square[][] squares = grid.getSquares();
        CrosswordSquare[][] crosswordSquares = new CrosswordSquare[grid.getNumRows()][grid.getNumCols()];
        for (int i = 0; i < grid.getNumRows(); i++)
            for (int j = 0; j < grid.getNumCols(); j++) {
                boolean isOpen = ImageUtils.isLight(squares[i][j].getRgb());
                boolean canGoAcross = isOpen && j < grid.getNumCols() - 1 && ImageUtils.isLight(squares[i][j + 1].getRgb())
                        && squares[i][j].getRightBorder().getStyle().compareTo(Style.THIN) <= 0;
                boolean canGoDown = isOpen && i < grid.getNumRows() - 1 && ImageUtils.isLight(squares[i + 1][j].getRgb())
                        && squares[i][j].getBottomBorder().getStyle().compareTo(Style.THIN) <= 0;
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
        ClueDirection currentDirection = ClueDirection.UNKNOWN;
        int currentClueNumber = -1;
        String currentClue = "";
        List<Clue> clues = new ArrayList<>();
        for (String line : text.split("\n")) {
            String normalizedLine = line.trim().toUpperCase();
            boolean isDirection = false;
            for (ClueDirection direction : DIRECTIONS)
                if (normalizedLine.startsWith(direction.toString())) {
                    clues.add(new Clue(currentDirection, currentClueNumber, currentClue));
                    currentDirection = direction;
                    currentClue = "";
                    isDirection = true;
                }
            if (!normalizedLine.isEmpty() && Character.isDigit(normalizedLine.charAt(0))) {
                clues.add(new Clue(currentDirection, currentClueNumber, currentClue));
                currentClue = "";
                currentClueNumber = Integer.parseInt(normalizedLine.split("\\D+")[0]);
            }
            if (!normalizedLine.isEmpty() && !isDirection)
                currentClue += normalizedLine + " ";
        }
        clues.add(new Clue(currentDirection, currentClueNumber, currentClue));

        Multimap<ClueDirection, Clue> cluesByDirection = Multimaps.index(clues, clue -> clue.getDirection());
        List<ClueSection> sections = new ArrayList<>();
        for (ClueDirection direction : DIRECTIONS) {
            List<NumberedClue> sectionClues = cluesByDirection.get(direction).stream()
                .filter(clue -> !clue.clue.isEmpty())
                .sorted(Comparator.comparing(clue -> clue.clueNumber))
                .map(clue -> new NumberedClue(clue.clueNumber, clue.clue.trim()))
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
