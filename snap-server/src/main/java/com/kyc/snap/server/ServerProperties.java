package com.kyc.snap.server;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ServerProperties(
        String draftSpreadsheetId,
        String googleServerScriptUrl,
        String hostingServerOrigin) {

    public static ServerProperties get() {
        try {
            return new JavaPropsMapper().readValue(new File("gradle.properties"), ServerProperties.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
