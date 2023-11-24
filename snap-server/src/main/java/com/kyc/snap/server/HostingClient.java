package com.kyc.snap.server;

import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.jackson.JacksonDecoder;

public class HostingClient {

    private final String serverOrigin = ServerProperties.get().hostingServerOrigin();

    /**
     * Hosts a file on the configured public server, and returns the URL to access the file.
     */
    public String hostFile(String contentType, byte[] data) {
        String hostingBaseUrl = getHostingBaseUrl();
        HostingClientService hosting = Feign.builder()
            .decoder(new JacksonDecoder())
            .target(HostingClientService.class, hostingBaseUrl);
        String fileId = hosting.uploadFile(contentType, data);
        return String.format("%s/%s", hostingBaseUrl, fileId);
    }

    interface HostingClientService {

        @RequestLine("POST /")
        @Headers("Content-type: {contentType}")
        String uploadFile(@Param("contentType") String contentType, byte[] data);
    }

    private String getHostingBaseUrl() {
        return String.format("%s/api/files", serverOrigin);
    }
}
