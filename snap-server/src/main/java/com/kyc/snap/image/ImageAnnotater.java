
package com.kyc.snap.image;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.imageio.ImageIO;

import com.kyc.snap.grid.Grid;
import com.kyc.snap.grid.Grid.Square;
import com.kyc.snap.grid.GridLines;
import com.kyc.snap.grid.GridPosition;
import com.kyc.snap.grid.GridPosition.Col;
import com.kyc.snap.grid.GridPosition.Row;

public class ImageAnnotater {

    private final BufferedImage image;
    private final int height;
    private final int width;
    private final Graphics g;

    public ImageAnnotater(BufferedImage image) {
        BufferedImage newImage = ImageUtils.copy(image);
        this.image = newImage;
        this.height = newImage.getHeight();
        this.width = newImage.getWidth();
        this.g = newImage.getGraphics();
    }

    public BufferedImage getImage() {
        return image;
    }

    public void markLines(GridLines lines) {
        g.setColor(Color.red);
        for (int y : lines.getHorizontalLines())
            g.drawLine(0, y, image.getWidth(), y);
        for (int x : lines.getVerticalLines())
            g.drawLine(x, 0, x, image.getHeight());
    }

    public void markPosition(GridPosition pos) {
        g.setColor(Color.red);
        for (Row row : pos.getRows())
            g.drawRect(0, row.getStartY(), width, row.getHeight());
        for (Col col : pos.getCols())
            g.drawRect(col.getStartX(), 0, col.getWidth(), height);
    }

    public void markGrid(GridPosition pos, Grid grid) {
        g.setFont(new Font("Helvetica", 0, 14));
        for (int i = 0; i < pos.getNumRows(); i++)
            for (int j = 0; j < pos.getNumCols(); j++) {
                Row row = pos.getRows().get(i);
                Col col = pos.getCols().get(j);
                Square square = grid.square(i, j);
                g.setColor(new Color(square.getRgb()));
                g.fillRect(
                    col.getStartX() + col.getWidth() / 3,
                    row.getStartY() + row.getHeight() / 3,
                    col.getWidth() / 3,
                    row.getHeight() / 3);
                g.setColor(Color.red);
                g.drawRect(
                    col.getStartX() + col.getWidth() / 3,
                    row.getStartY() + row.getHeight() / 3,
                    col.getWidth() / 3,
                    row.getHeight() / 3);
                g.setColor(Color.blue);
                g.drawString(
                    square.getText(),
                    col.getStartX() + col.getWidth() / 3,
                    row.getStartY() + 2 * row.getHeight() / 3);
            }
    }

    public void open() {
        try {
            File file = Files.createTempFile("", ".png").toFile();
            ImageIO.write(image, "png", file);
            Desktop.getDesktop().open(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
