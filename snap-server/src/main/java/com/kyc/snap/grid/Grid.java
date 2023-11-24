package com.kyc.snap.grid;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

public record Grid(int numRows, int numCols, Square[][] squares) {

    public static Grid create(int numRows, int numCols) {
        Square[][] squares = new Square[numRows][numCols];
        for (int i = 0; i < numRows; i++)
            for (int j = 0; j < numCols; j++)
                squares[i][j] = new Square();
        return new Grid(numRows, numCols, squares);
    }

    @JsonIgnore
    public Square square(int row, int col) {
        return squares[row][col];
    }

    @Data
    public static class Square {

        private int rgb = -1;
        private String text = "";
        private Border topBorder = Border.NONE;
        private Border rightBorder = Border.NONE;
        private Border bottomBorder = Border.NONE;
        private Border leftBorder = Border.NONE;

        @JsonIgnore
        public List<Border> borders() {
            return List.of(topBorder, rightBorder, bottomBorder, leftBorder);
        }
    }
}
