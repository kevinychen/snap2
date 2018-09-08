package com.kyc.snap.grid;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Preconditions;
import com.kyc.snap.grid.GridPosition.Col;
import com.kyc.snap.grid.GridPosition.Row;
import com.kyc.snap.opencv.OpenCvManager;
import com.kyc.snap.opencv.OpenCvManager.Line;

import lombok.Data;

@Data
public class GridParser {

    private final OpenCvManager openCv;

    public GridPosition findGridPosition(BufferedImage image, int approxGridSquareSize) {
        int minGridSquareSize = approxGridSquareSize / 2;

        List<Line> cardinalLines = openCv.findLines(image, Math.PI / 2, minGridSquareSize);

        List<Integer> xs = new ArrayList<>();
        List<Integer> ys = new ArrayList<>();
        for (Line line : cardinalLines) {
            boolean vertical = line.getX1() == line.getX2();
            boolean horizontal = line.getY1() == line.getY2();
            Preconditions.checkArgument(horizontal != vertical,
                "Expected horizontal or vertical line but got %s", line);
            if (vertical)
                xs.add((int) line.getX1());
            else
                ys.add((int) line.getY1());
        }

        Collections.sort(xs);
        Collections.sort(ys);

        List<Row> rows = new ArrayList<>();
        List<Col> cols = new ArrayList<>();
        for (int i = 0; i + 1 < xs.size(); i++)
            if (xs.get(i + 1) - xs.get(i) >= minGridSquareSize)
                cols.add(new Col(xs.get(i), xs.get(i + 1) - xs.get(i)));
        for (int i = 0; i + 1 < ys.size(); i++)
            if (ys.get(i + 1) - ys.get(i) >= minGridSquareSize)
                rows.add(new Row(ys.get(i), ys.get(i + 1) - ys.get(i)));
        return new GridPosition(rows, cols);
    }
}
