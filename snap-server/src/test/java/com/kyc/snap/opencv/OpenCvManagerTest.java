package com.kyc.snap.opencv;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.Test;

import com.kyc.snap.opencv.OpenCvManager.Line;

public class OpenCvManagerTest {

    final OpenCvManager openCv = new OpenCvManager();

    @Test
    public void testFindLines() {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_3BYTE_BGR);
        image.getGraphics().fillRect(50, 0, 50, 100);
        List<Line> lines = openCv.findLines(image, Math.PI / 2, 99);
        assertThat(lines).containsExactly(new Line(49., 99., 49., 0.));
    }
}
