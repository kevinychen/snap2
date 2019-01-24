package com.kyc.snap.google;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.slides.v1.Slides;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;

public class GoogleAPIManager {

    public static final String CREDENTIALS_FILE = "./google-api-credentials.json";

    private static final String APPLICATION_NAME = "Snap";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final int DEFAULT_SHEET_ID = 0;

    private final GoogleCredential credential;
    private final Drive drive;
    private final Sheets sheets;
    private final Slides slides;

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

    public SpreadsheetManager createSheet(String folderId) {
        try {
            Spreadsheet spreadsheet = sheets.spreadsheets().create(new Spreadsheet()).execute();
            File file = drive.files().get(spreadsheet.getSpreadsheetId()).setFields("parents").execute();
            drive.files().update(spreadsheet.getSpreadsheetId(), null)
                .setAddParents(folderId)
                .setRemoveParents(Joiner.on(", ").join(file.getParents()))
                .setFields("id, parents")
                .execute();
            return new SpreadsheetManager(credential, sheets.spreadsheets(), spreadsheet.getSpreadsheetId(), DEFAULT_SHEET_ID);
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
}
