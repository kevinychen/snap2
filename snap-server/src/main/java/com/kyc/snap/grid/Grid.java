package com.kyc.snap.grid;

import lombok.Data;

@Data
public class Grid {

    private final Square[][] squares;

    public Square getSquare(int row, int col) {
        return squares[row][col];
    }

    @Data
    public static class Square {

        private final int rgb;
        private final String text;
    }
}
