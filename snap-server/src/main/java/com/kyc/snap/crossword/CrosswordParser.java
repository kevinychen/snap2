package com.kyc.snap.crossword;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.kyc.snap.crossword.Crossword.Entry;
import com.kyc.snap.crossword.CrosswordClues.CluePosition;
import com.kyc.snap.grid.Grid;
import com.kyc.snap.grid.Grid.Square;
import com.kyc.snap.grid.GridPosition;
import com.kyc.snap.image.ImageUtils;

import lombok.Data;

public class CrosswordParser {

    public Crossword parseCrossword(GridPosition pos, Grid grid) {
        Square[][] squares = grid.getSquares();
        CrosswordSquare[][] crosswordSquares = new CrosswordSquare[pos.getNumRows()][pos.getNumCols()];
        for (int i = 0; i < pos.getNumRows(); i++)
            for (int j = 0; j < pos.getNumCols(); j++) {
                boolean isOpen = ImageUtils.isLight(squares[i][j].getRgb());
                boolean canGoAcross = isOpen && j < pos.getNumCols() - 1 && ImageUtils.isLight(squares[i][j + 1].getRgb());
                boolean canGoDown = isOpen && i < pos.getNumRows() - 1 && ImageUtils.isLight(squares[i + 1][j].getRgb());
                crosswordSquares[i][j] = new CrosswordSquare(isOpen, canGoAcross, canGoDown);
            }

        int clueNumber = 1;
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < pos.getNumRows(); i++)
            for (int j = 0; j < pos.getNumCols(); j++) {
                CrosswordSquare square = crosswordSquares[i][j];
                boolean hasEntry = false;
                if (square.canGoAcross && (j == 0 || !crosswordSquares[i][j - 1].canGoAcross)) {
                    int numSquares = 2;
                    while (crosswordSquares[i][j + numSquares - 1].canGoAcross)
                        numSquares++;
                    entries.add(new Entry(i, j, numSquares, ClueDirection.ACROSS, clueNumber + "", ""));
                    hasEntry = true;
                }
                if (square.canGoDown && (i == 0 || !crosswordSquares[i - 1][j].canGoDown)) {
                    int numSquares = 2;
                    while (crosswordSquares[i + numSquares - 1][j].canGoDown)
                        numSquares++;
                    entries.add(new Entry(i, j, numSquares, ClueDirection.DOWN, clueNumber + "", ""));
                    hasEntry = true;
                }
                if (hasEntry)
                    clueNumber++;
            }

        return new Crossword(pos.getNumRows(), pos.getNumCols(), entries);
    }

    public CrosswordClues parseClues(String text) {
        ClueDirection currentDirection = null;
        String currentClueNumber = null;
        String currentClue = "";
        Map<CluePosition, String> clues = new HashMap<>();
        for (String line : text.split("\n")) {
            String normalizedLine = line.trim().toUpperCase();
            if (!currentClue.isEmpty() && (normalizedLine.startsWith("ACROSS") || normalizedLine.startsWith("DOWN")
                    || Character.isDigit(normalizedLine.charAt(0)))) {
                Preconditions.checkArgument(currentDirection != null, "No clue direction set");
                Preconditions.checkArgument(currentClueNumber != null, "No clue number set");
                clues.put(new CluePosition(currentDirection, currentClueNumber), currentClue);
                currentClue = "";
            }

            if (normalizedLine.equalsIgnoreCase("ACROSS"))
                currentDirection = ClueDirection.ACROSS;
            else if (normalizedLine.equalsIgnoreCase("DOWN"))
                currentDirection = ClueDirection.DOWN;
            else {
                if (Character.isDigit(normalizedLine.charAt(0)))
                    currentClueNumber = normalizedLine.split("\\D+")[0];
                currentClue += line;
            }
        }
        clues.put(new CluePosition(currentDirection, currentClueNumber), currentClue);
        return new CrosswordClues(clues);
    }

    @Data
    private static class CrosswordSquare {

        private final boolean isOpen;
        private final boolean canGoAcross;
        private final boolean canGoDown;
    }
}
