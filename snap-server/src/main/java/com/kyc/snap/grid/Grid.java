package com.kyc.snap.grid;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableList;

import lombok.Data;

@Data
public class Grid {

    private final int numRows;
    private final int numCols;
    private final Square[][] squares;

    public Grid(int numRows, int numCols) {
        this.numRows = numRows;
        this.numCols = numCols;
        this.squares = new Square[numRows][numCols];

        for (int i = 0; i < numRows; i++)
            for (int j = 0; j < numCols; j++)
                squares[i][j] = new Square();
    }

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
            return ImmutableList.of(topBorder, rightBorder, bottomBorder, leftBorder);
        }
    }
}
