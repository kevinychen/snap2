
package com.kyc.snap.google;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.api.services.slides.v1.Slides.Presentations;
import com.google.api.services.slides.v1.model.BatchUpdatePresentationRequest;
import com.google.api.services.slides.v1.model.CreateImageRequest;
import com.google.api.services.slides.v1.model.PageElementProperties;
import com.kyc.snap.image.ImageUtils;
import com.kyc.snap.server.HostingClient;

import lombok.Data;

@Data
public class PresentationManager {

    private final Presentations presentations;
    private final String presentationId;
    private final String slideId;

    public void addImages(List<PositionedImage> images, HostingClient hosting) {
        List<HostedImage> hostedImages = new ArrayList<>();
        for (PositionedImage image : images) {
            String url = hosting.hostFile("image/png", ImageUtils.toBytes(image.image));
            hostedImages.add(new HostedImage(image, url));
        }
        try {
            presentations
                .batchUpdate(presentationId, new BatchUpdatePresentationRequest()
                    .setRequests(hostedImages.stream().map(image -> new com.google.api.services.slides.v1.model.Request()
                        .setCreateImage(new CreateImageRequest()
                            .setElementProperties(new PageElementProperties().setPageObjectId(slideId))
                            .setUrl(image.url)))
                        .collect(Collectors.toList())))
                .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Data
    public static class PositionedImage {

        private final BufferedImage image;
        private final double xPercent;
        private final double yPercent;
    }

    @Data
    private static class HostedImage {

        private final PositionedImage image;
        private final String url;
    }
}
