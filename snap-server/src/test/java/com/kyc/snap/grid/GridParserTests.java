package com.kyc.snap.grid;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import com.kyc.snap.document.Pdf;
import com.kyc.snap.opencv.OpenCvManager;
import javax.imageio.ImageIO;

public class GridParserTests {

    final GridParser gridParser = new GridParser(new OpenCvManager());

    @Test
    public void testFindGridLines() throws IOException {
        testFindGridLinesHelper("crosswerd.png", 15, 16);
        testFindGridLinesHelper("hugs.png", 19, 13);
        testFindGridLinesHelper("role_call.png", 18, 42);
        testFindGridLinesHelper("zsasz.png", 13, 14);
        testFindGridLinesHelper("kitchen_rush.pdf", 17, 17);
        testFindGridLinesHelper("pit.pdf", 12, 12);
        testFindGridLinesHelper("meatballs.pdf", 17, 17);
        testFindGridLinesHelper("hugs.pdf", 19, 13);
    }

    @Test
    public void testFindImplicitGridLines() throws IOException {
        BufferedImage image = getImage("./src/test/resources/blood_bowl.png");
        GridLines gridLines = gridParser.findImplicitGridLines(image);
        assertThat(gridLines.horizontalLines().size() - 1).isEqualTo(18);
        assertThat(gridLines.verticalLines().size() - 1).isEqualTo(24);
    }

    void testFindGridLinesHelper(String filename, int numRows, int numCols) throws IOException {
        BufferedImage image = getImage("./src/test/resources/" + filename);
        GridLines gridLines = gridParser.findGridLines(image);
        gridLines = gridParser.getInterpolatedGridLines(gridLines);
        assertThat(gridLines.horizontalLines().size() - 1).isEqualTo(numRows);
        assertThat(gridLines.verticalLines().size() - 1).isEqualTo(numCols);
    }

    BufferedImage getImage(String path) throws IOException {
        if (path.endsWith(".png")) {
            return ImageIO.read(new File(path));
        } else if (path.endsWith(".pdf")) {
            try (InputStream in = new FileInputStream(path); Pdf pdf = new Pdf(in)) {
                return pdf.toImage(0);
            }
        }
        throw new IllegalArgumentException();
    }
}
