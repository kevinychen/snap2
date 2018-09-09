package com.kyc.snap.crossword;

import java.util.ArrayList;
import java.util.List;

import com.kyc.snap.crossword.Crossword.Direction;
import com.kyc.snap.crossword.Crossword.Entry;
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
                    entries.add(new Entry(i, j, numSquares, Direction.ACROSS, clueNumber + "", ""));
                    hasEntry = true;
                }
                if (square.canGoDown && (i == 0 || !crosswordSquares[i - 1][j].canGoDown)) {
                    int numSquares = 2;
                    while (crosswordSquares[i + numSquares - 1][j].canGoDown)
                        numSquares++;
                    entries.add(new Entry(i, j, numSquares, Direction.DOWN, clueNumber + "", ""));
                    hasEntry = true;
                }
                if (hasEntry)
                    clueNumber++;
            }

        return new Crossword(pos.getNumRows(), pos.getNumCols(), entries);
    }

    @Data
    private static class CrosswordSquare {

        private final boolean isOpen;
        private final boolean canGoAcross;
        private final boolean canGoDown;
    }
}
