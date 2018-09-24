package com.kyc.snap.google;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.slides.v1.Slides;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.protobuf.ByteString;
import com.kyc.snap.image.ImageUtils;

public class GoogleAPIManager {

    public static final String CREDENTIALS_FILE = "./google-api-credentials.json";

    private static final String APPLICATION_NAME = "Snap";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final Feature TEXT_DETECTION_FEATURE = Feature.newBuilder().setType(Type.TEXT_DETECTION).build();
    private static final int TEXT_DETECTION_IMAGE_LIMIT = 16; // https://cloud.google.com/vision/quotas

    private final GoogleCredential credential;
    private final Drive drive;
    private final Sheets sheets;
    private final Slides slides;
    private final ImageAnnotatorSettings imageAnnotatorSettings;

    public GoogleAPIManager() {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            credential = GoogleCredential.fromStream(new FileInputStream(CREDENTIALS_FILE))
                .createScoped(ImmutableSet.of(SheetsScopes.DRIVE));
            drive = new Drive.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
            sheets = new Sheets.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
            slides = new Slides.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(CREDENTIALS_FILE));
            imageAnnotatorSettings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Anyone with a link to the file is given the specified role (either "writer", "reader", or null).
     */
    public void grantLinkPermissions(String fileId, String role) {
        try {
            drive.permissions().update(fileId, "anyoneWithLink", new Permission().setRole(role)).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public SpreadsheetManager getSheet(String spreadsheetId, int sheetId) {
        return new SpreadsheetManager(credential, sheets.spreadsheets(), spreadsheetId, sheetId);
    }

    public PresentationManager getPresentation(String presentationId, String slideId) {
        return new PresentationManager(slides.presentations(), presentationId, slideId);
    }

    public Map<BufferedImage, String> batchFindText(Collection<BufferedImage> images) {
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create(imageAnnotatorSettings)) {
            Map<BufferedImage, String> result = new HashMap<>();
            for (List<BufferedImage> partition : Iterables.partition(images, TEXT_DETECTION_IMAGE_LIMIT)) {
                BatchAnnotateImagesResponse response = client.batchAnnotateImages(partition.stream()
                    .map(image -> AnnotateImageRequest.newBuilder()
                        .addFeatures(TEXT_DETECTION_FEATURE)
                        .setImage(Image.newBuilder().setContent(ByteString.copyFrom(ImageUtils.toBytes(image))).build())
                        .build())
                        .collect(Collectors.toList()));
                List<String> texts = response.getResponsesList().stream()
                        .map(res -> res.getTextAnnotationsList().stream()
                            .map(EntityAnnotation::getDescription)
                            .max(Comparator.comparing(String::length))
                            .orElse(""))
                        .collect(Collectors.toList());
                for (int i = 0; i < partition.size(); i++)
                    result.put(partition.get(i), texts.get(i));
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
