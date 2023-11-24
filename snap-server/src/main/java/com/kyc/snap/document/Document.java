package com.kyc.snap.document;

import java.util.List;

public record Document(String id, List<DocumentPage> pages) {

    /**
     * @param compressedImageId A compressed image to send over the network
     */
    public record DocumentPage(String imageId, String compressedImageId, double scale, List<DocumentText> texts) {}

    public record DocumentText(String text, Rectangle bounds) {}
}
