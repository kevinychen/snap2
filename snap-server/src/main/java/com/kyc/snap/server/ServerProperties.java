package com.kyc.snap.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import lombok.Data;

@Data
public class ServerProperties {

    private static ServerProperties INSTANCE = new ServerProperties();

    private final String draftSpreadsheetId;
    private final String googleServerScriptUrl;
    private final String hostingServerOrigin;

    private ServerProperties() {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream("gradle.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        draftSpreadsheetId = props.getProperty("draftSpreadsheetId");
        googleServerScriptUrl = props.getProperty("googleServerScriptUrl");
        hostingServerOrigin = props.getProperty("hostingServerOrigin");
    }

    public static ServerProperties get() {
        return INSTANCE;
    }
}
