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

    public String hostResource(String contentType, byte[] data) {
        String hostingBaseUrl = getHostingBaseUrl();
        HostingClientService hosting = Feign.builder()
            .decoder(new JacksonDecoder())
            .target(HostingClientService.class, hostingBaseUrl);
        String resourceId = hosting.hostResource(contentType, data).getValue();
        return String.format("%s/%s", hostingBaseUrl, resourceId);
    }

    interface HostingClientService {

        @RequestLine("POST /")
        @Headers("Content-type: {contentType}")
        StringJson hostResource(@Param("contentType") String contentType, byte[] data);
    }

    private String getHostingBaseUrl() {
        return String.format("http://%s/api/hosting", serverSocketAddress);
    }
}
