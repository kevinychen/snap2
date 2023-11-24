package com.kyc.snap.grid;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record Grid(int numRows, int numCols, Square[][] squares) {

    public Grid(int numRows, int numCols) {
        this(numRows, numCols, new Square[numRows][numCols]);
        for (int i = 0; i < numRows; i++)
            for (int j = 0; j < numCols; j++)
                squares[i][j] = new Square();
    }

    @JsonIgnore
    public Square square(int row, int col) {
        return squares[row][col];
    }

    public static class Square {

        public int rgb = -1;
        public String text = "";
        public Border topBorder = Border.NONE;
        public Border rightBorder = Border.NONE;
        public Border bottomBorder = Border.NONE;
        public Border leftBorder = Border.NONE;

        @JsonIgnore
        public List<Border> borders() {
            return List.of(topBorder, rightBorder, bottomBorder, leftBorder);
        }
    }
}
