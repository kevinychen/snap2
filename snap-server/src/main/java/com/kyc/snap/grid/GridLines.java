package com.kyc.snap.grid;

import java.util.TreeSet;

public record GridLines(TreeSet<Integer> horizontalLines, TreeSet<Integer> verticalLines) {}
