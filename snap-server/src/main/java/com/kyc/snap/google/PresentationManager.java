
package com.kyc.snap.google;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.api.services.slides.v1.Slides.Presentations;
import com.google.api.services.slides.v1.model.AffineTransform;
import com.google.api.services.slides.v1.model.BatchUpdatePresentationRequest;
import com.google.api.services.slides.v1.model.CreateImageRequest;
import com.google.api.services.slides.v1.model.Dimension;
import com.google.api.services.slides.v1.model.PageElementProperties;
import com.google.api.services.slides.v1.model.Request;
import com.google.api.services.slides.v1.model.Size;
import com.kyc.snap.image.ImageUtils;
import com.kyc.snap.server.HostingClient;

import lombok.Data;

@Data
public class PresentationManager {

    private final Presentations presentations;
    private final String presentationId;
    private final String slideId;

    public void addImages(List<PositionedImage> images, int parentWidth, int parentHeight) {
        HostingClient hosting = new HostingClient();
        List<HostedImage> hostedImages = new ArrayList<>();
        for (PositionedImage image : images) {
            String url = hosting.hostFile("image/png", ImageUtils.toBytes(image.image));
            hostedImages.add(new HostedImage(image, url));
        }
        try {
            Size slideSize = presentations.get(presentationId).execute().getPageSize();
            double slideWidth = toPts(slideSize.getWidth());
            double slideHeight = toPts(slideSize.getHeight());
            double scale = Math.min(slideWidth / parentWidth, slideHeight / parentHeight);
            presentations
                .batchUpdate(presentationId, new BatchUpdatePresentationRequest()
                    .setRequests(hostedImages.stream()
                        .map(image -> new Request()
                            .setCreateImage(new CreateImageRequest()
                                .setElementProperties(new PageElementProperties()
                                    .setPageObjectId(slideId)
                                    .setTransform(new AffineTransform()
                                        .setTranslateX(image.getImage().x * scale)
                                        .setTranslateY(image.getImage().y * scale)
                                        .setScaleX(scale)
                                        .setScaleY(scale)
                                        .setUnit("PT")))
                                .setUrl(image.url)))
                        .collect(Collectors.toList())))
                .execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static double toPts(Dimension dimension) {
        switch (dimension.getUnit()) {
            case "EMU":
                return dimension.getMagnitude() / 12700;
            case "PT":
                return dimension.getMagnitude();
            default:
                throw new RuntimeException("Invalid dimension unit: " + dimension.getUnit());
        }
    }

    @Data
    public static class PositionedImage {

        private final BufferedImage image;
        private final int x;
        private final int y;
    }

    @Data
    private static class HostedImage {

        private final PositionedImage image;
        private final String url;
    }
}
