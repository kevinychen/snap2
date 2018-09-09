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
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Spreadsheet;
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

    public static final String DEFAULT_CREDENTIALS_FILE = "./google-api-credentials.json";
    public static final String SAMPLE_SPREADSHEET_ID = "1n2XG8kgi-XZoD1n5jZoW4UbIFI99U2l0Uc_9SQPb8TA";
    public static final int SAMPLE_SHEET_ID = 0;

    private static final String APPLICATION_NAME = "Snap";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final Feature TEXT_DETECTION_FEATURE = Feature.newBuilder().setType(Type.TEXT_DETECTION).build();
    private static final int TEXT_DETECTION_IMAGE_LIMIT = 16; // https://cloud.google.com/vision/quotas

    private final Sheets sheets;
    private final ImageAnnotatorSettings imageAnnotatorSettings;

    public GoogleAPIManager(String credentialsFile) {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(credentialsFile))
                .createScoped(ImmutableSet.of(SheetsScopes.DRIVE));
            sheets = new Sheets.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
            GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsFile));
            imageAnnotatorSettings = ImageAnnotatorSettings.newBuilder()
                    .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                    .build();
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public SpreadsheetManager getSheet(String spreadsheetId) {
        try {
            Spreadsheet spreadsheet = sheets.spreadsheets().get(spreadsheetId).execute();
            return new SpreadsheetManager(sheets.spreadsheets(), spreadsheet);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
