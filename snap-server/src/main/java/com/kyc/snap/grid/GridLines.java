package com.kyc.snap.grid;

import java.util.TreeSet;

import lombok.Data;

@Data
public class GridLines {

    private final TreeSet<Integer> horizontalLines;
    private final TreeSet<Integer> verticalLines;
}
