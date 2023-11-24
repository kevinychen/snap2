
package com.kyc.snap.google;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
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

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;

public record PresentationManager(
        GoogleCredential credential,
        Presentations presentations,
        String presentationId,
        String slideId) {

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
                                                            .setTranslateX(image.image().x * scale)
                                                            .setTranslateY(image.image().y * scale)
                                                            .setScaleX(scale)
                                                            .setScaleY(scale)
                                                            .setUnit("PT")))
                                            .setUrl(image.url)))
                            .toList()))
                    .execute();
        } catch (GoogleJsonResponseException e) {
            throw toClientErrorException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ClientErrorException toClientErrorException(GoogleJsonResponseException e) {
        if (e.getStatusCode() == 403) {
            return new ForbiddenException(
                    String.format("Insufficient permissions. "
                                    + "Please grant '%s' edit permissions to your Google slide or a parent folder and try again.",
                            credential.getServiceAccountId()),
                    e);
        }
        return new ClientErrorException(Response.Status.fromStatusCode(e.getStatusCode()), e);
    }

    private static double toPts(Dimension dimension) {
        return switch (dimension.getUnit()) {
            case "EMU" -> dimension.getMagnitude() / 12700;
            case "PT" -> dimension.getMagnitude();
            default -> throw new RuntimeException("Invalid dimension unit: " + dimension.getUnit());
        };
    }

    public record PositionedImage(BufferedImage image, int x, int y) {}

    private record HostedImage(PositionedImage image, String url) {}
}
