package com.kyc.snap.grid;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import com.kyc.snap.google.SpreadsheetManager;
import com.kyc.snap.google.SpreadsheetManager.ValueCell;
import com.kyc.snap.server.SnapConfiguration;

import lombok.Data;

@Data
public class GridPositionSpreadsheetWrapper {

    private final SnapConfiguration configuration;
    private final SpreadsheetManager spreadsheets;

    public void toSpreadsheet(String sessionId, BufferedImage image, GridPosition pos) {
        spreadsheets.clear();
        spreadsheets.setAllColumnWidths(20);

        List<ValueCell> formulaCells = new ArrayList<>();
        for (int i = 0; i < pos.getNumRows(); i++)
            for (int j = 0; j < pos.getNumCols(); j++) {
                formulaCells.add(new ValueCell(i, j, String.format("=IMAGE(\"http://%s/api/session/%s/images/%d/%d.png\")",
                    configuration.getSocketAddress(),
                    sessionId,
                    i,
                    j)));
            }
        spreadsheets.setFormulas(formulaCells);
    }
}