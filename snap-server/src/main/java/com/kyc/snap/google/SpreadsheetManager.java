package com.kyc.snap.google;

import com.google.api.services.sheets.v4.Sheets.Spreadsheets;
import com.google.api.services.sheets.v4.model.Spreadsheet;

import lombok.Data;

@Data
public class SpreadsheetManager {

    private final Spreadsheets spreadsheets;
    private final Spreadsheet spreadsheet;

    public String getUrl() {
        return spreadsheet.getSpreadsheetUrl();
    }
}
