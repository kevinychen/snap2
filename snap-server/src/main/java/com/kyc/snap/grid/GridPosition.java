package com.kyc.snap.grid;

import java.util.List;

import lombok.Data;

@Data
public class GridPosition {

    private final List<Row> rows;
    private final List<Col> cols;

    public int getNumRows() {
        return rows.size();
    }

    public int getNumCols() {
        return cols.size();
    }

    @Data
    public static class Row {

        private final int startY;
        private final int height;
    }

    @Data
    public static class Col {

        private final int startX;
        private final int width;
    }
}
