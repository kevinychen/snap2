package com.kyc.snap.google;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.common.collect.ImmutableSet;

public class GoogleAPIManager {

    public static final String DEFAULT_CREDENTIALS_FILE = "./google-api-credentials.json";
    public static final String SAMPLE_SPREADSHEET_ID = "1n2XG8kgi-XZoD1n5jZoW4UbIFI99U2l0Uc_9SQPb8TA";
    public static final int SAMPLE_SHEET_ID = 0;

    private static final String APPLICATION_NAME = "Snap";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private final Sheets sheets;

    public GoogleAPIManager(String credentialsFile) {
        try {
            HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleCredential credential = GoogleCredential.fromStream(new FileInputStream(credentialsFile))
                .createScoped(ImmutableSet.of(SheetsScopes.DRIVE));
            sheets = new Sheets.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
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
}
