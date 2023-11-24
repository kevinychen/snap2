package com.kyc.snap.grid;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record GridPosition(List<Row> rows, List<Col> cols) {

    @JsonIgnore
    public int getNumRows() {
        return rows.size();
    }

    @JsonIgnore
    public int getNumCols() {
        return cols.size();
    }

    public record Row(int startY, int height) {}

    public record Col(int startX, int width) {}
}
