
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

    public void drawLine(int x1, int y1, int x2, int y2) {
        g.setColor(Color.red);
        g.drawLine(x1, y1, x2, y2);
    }

    public void markLines(GridLines lines) {
        g.setColor(Color.red);
        for (int y : lines.horizontalLines())
            g.drawLine(0, y, image.getWidth(), y);
        for (int x : lines.verticalLines())
            g.drawLine(x, 0, x, image.getHeight());
    }

    public void markPosition(GridPosition pos) {
        g.setColor(Color.red);
        for (Row row : pos.rows())
            g.drawRect(0, row.startY(), width, row.height());
        for (Col col : pos.cols())
            g.drawRect(col.startX(), 0, col.width(), height);
    }

    public void markGrid(GridPosition pos, Grid grid) {
        g.setFont(new Font("Helvetica", Font.PLAIN, 14));
        for (int i = 0; i < pos.getNumRows(); i++)
            for (int j = 0; j < pos.getNumCols(); j++) {
                Row row = pos.rows().get(i);
                Col col = pos.cols().get(j);
                Square square = grid.square(i, j);

                g.setColor(new Color(square.getRgb()));
                g.fillRect(
                    col.startX() + col.width() / 3,
                    row.startY() + row.height() / 3,
                    col.width() / 3,
                    row.height() / 3);
                g.setColor(Color.red);
                g.drawRect(
                    col.startX() + col.width() / 3,
                    row.startY() + row.height() / 3,
                    col.width() / 3,
                    row.height() / 3);

                g.setColor(Color.blue);
                g.drawString(
                    square.getText(),
                    col.startX() + col.width() / 3,
                    row.startY() + 2 * row.height() / 3);

                g.setColor(new Color(square.getRightBorder().getRgb()));
                g.fillRect(
                    col.startX() + col.width() * 2 / 3 + 1,
                    row.startY() + row.height() / 3,
                    square.getRightBorder().getWidth(),
                    row.height() / 3);
                g.setColor(new Color(square.getBottomBorder().getRgb()));
                g.fillRect(
                    col.startX() + col.width() / 3,
                    row.startY() + row.height() * 2 / 3 + 1,
                    col.width() / 3,
                    square.getBottomBorder().getWidth());
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
