package com.kyc.snap.server;

import java.awt.image.BufferedImage;

import com.kyc.snap.grid.Grid;
import com.kyc.snap.grid.GridLines;
import com.kyc.snap.grid.GridPosition;

import lombok.Data;

@Data
public class ImageSession {

    private final String sessionId;
    private final BufferedImage image;

    private GridLines lines;
    private GridPosition pos;
    private Grid grid;

    private int approxGridSize = 32;
    private String spreadsheetId = "1n2XG8kgi-XZoD1n5jZoW4UbIFI99U2l0Uc_9SQPb8TA";
    private int sheetId = 0;
}
