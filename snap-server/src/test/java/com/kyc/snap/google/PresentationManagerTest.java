package com.kyc.snap.google;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.junit.Test;

import com.kyc.snap.google.PresentationManager.PositionedImage;
import com.kyc.snap.image.ImageBlob;
import com.kyc.snap.image.ImageUtils;
import com.kyc.snap.server.HostingClient;

public class PresentationManagerTest {

    HostingClient hosting = new HostingClient();

    String presentationId = "1z1Xph_GGr8vg7IQQfoUSCoZq-XuP4df1Bb7Mrm-t0v8";
    String slideId = "p";

    GoogleAPIManager googleApi = new GoogleAPIManager();
    PresentationManager presentations = googleApi.getPresentation(presentationId, slideId);

    File nationsFile = new File("src/test/resources/nations.png");
    int backgroundRgb = -1644826;
    int minBlobSize = 6;

    @Test
    public void testAddImagePieces() throws IOException {
        BufferedImage image = ImageIO.read(nationsFile);
        List<PositionedImage> pieces = new ArrayList<>();
        for (ImageBlob blob : ImageUtils.findBlobs(image, rgb -> rgb != backgroundRgb))
            if (blob.getX() > 1 && blob.getX() + blob.getWidth() < image.getWidth() - 1
                    && blob.getY() > 1 && blob.getY() + blob.getHeight() < image.getHeight() - 1
                    && blob.getWidth() >= minBlobSize && blob.getHeight() >= minBlobSize) {
                pieces.add(new PositionedImage(ImageUtils.getBlobImage(image, blob), 0, 0));
            }
        presentations.addImages(pieces, image.getWidth(), image.getHeight());
    }
}
