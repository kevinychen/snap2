package com.kyc.snap.server;

import java.util.UUID;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class HostingResource implements HostingService {

    private Cache<String, byte[]> resources = CacheBuilder.newBuilder()
            .maximumSize(256)
            .<String, byte[]>build();

    @Override
    public StringJson hostResource(byte[] data) {
        String resourceId = UUID.randomUUID().toString();
        resources.put(resourceId, data);
        return new StringJson(resourceId);
    }

    @Override
    public byte[] getResource(String resourceId) {
        return resources.getIfPresent(resourceId);
    }
}
