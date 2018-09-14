package com.kyc.snap.server;

import io.dropwizard.Configuration;

public class SnapConfiguration extends Configuration {

    private String socketAddress;

    /**
     * The hostname and port of the server, e.g. localhost:8080.
     */
    public String getSocketAddress() {
        return socketAddress;
    }

    public void setSocketAddress(String socketAddress) {
        this.socketAddress = socketAddress;
    }
}
