package com.kyc.snap.image;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.junit.Test;

import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;

public class ImageUtilsTests {

    @Test
    public void testFindBlobs() throws IOException {
        BufferedImage image = ImageIO.read(new File("./src/test/resources/mats.png"));
        assertThat(ImageUtils.findBlobs(image, true)).hasSize(19);
    }
}
