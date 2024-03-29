package com.kyc.snap.google;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Set;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.slides.v1.Slides;

public class GoogleAPIManager {

    public static final String CREDENTIALS_FILE = "./google-api-credentials.json";

    private static final String APPLICATION_NAME = "Snap";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private final GoogleCredential credential;
    private final Sheets sheets;
    private final Slides slides;

    public GoogleAPIManager() {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            credential = GoogleCredential.fromStream(new FileInputStream(CREDENTIALS_FILE))
                .createScoped(Set.of(SheetsScopes.DRIVE));
            sheets = new Sheets.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
            slides = new Slides.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public SpreadsheetManager getSheet(String spreadsheetId, int sheetId) {
        return new SpreadsheetManager(credential, sheets.spreadsheets(), spreadsheetId, sheetId);
    }

    public PresentationManager getPresentation(String presentationId, String slideId) {
        return new PresentationManager(credential, slides.presentations(), presentationId, slideId);
    }
}
