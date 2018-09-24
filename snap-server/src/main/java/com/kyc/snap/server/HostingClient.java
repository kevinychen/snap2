package com.kyc.snap.server;

import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.jackson.JacksonDecoder;
import lombok.Data;

@Data
public class HostingClient {

    private final String serverSocketAddress;

    /**
     * Hosts a resource on the configured public server, and returns the URL to access the resource.
     */
    public String hostResource(String contentType, byte[] data) {
        String hostingBaseUrl = getHostingBaseUrl();
        HostingClientService hosting = Feign.builder()
            .decoder(new JacksonDecoder())
            .target(HostingClientService.class, hostingBaseUrl);
        String resourceId = hosting.hostResource(contentType, data);
        return String.format("%s/%s", hostingBaseUrl, resourceId);
    }

    interface HostingClientService {

        @RequestLine("POST /")
        @Headers("Content-type: {contentType}")
        String hostResource(@Param("contentType") String contentType, byte[] data);
    }

    private String getHostingBaseUrl() {
        return String.format("http://%s/api/hosting", serverSocketAddress);
    }
}
