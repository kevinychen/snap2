package com.kyc.snap.grid;

import lombok.Data;

@Data
public class Grid {

    private final Square[][] squares;

    public int getNumRows() {
        return squares.length;
    }

    public int getNumCols() {
        return squares[0].length;
    }

    public Square getSquare(int row, int col) {
        return squares[row][col];
    }

    @Data
    public static class Square {

        private final int rgb;
        private final String text;
    }
}
