package com.kyc.snap.server;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import lombok.Data;

@Data
public class ServerProperties {

    private static ServerProperties INSTANCE = new ServerProperties();

    private final String googleServerScriptUrl;
    private final String hostingServerOrigin;

    private ServerProperties() {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(new java.io.File("server.properties")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        googleServerScriptUrl = props.getProperty("googleServerScriptUrl");
        hostingServerOrigin = props.getProperty("hostingServerOrigin");
    }

    public static ServerProperties get() {
        return INSTANCE;
    }
}
