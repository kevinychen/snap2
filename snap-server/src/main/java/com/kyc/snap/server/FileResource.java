package com.kyc.snap.server;

import com.kyc.snap.api.FileService;
import com.kyc.snap.store.Store;

public record FileResource(Store store) implements FileService {

    @Override
    public StringJson uploadFile(byte[] data) {
        String id = store.storeBlob(data);
        return new StringJson(id);
    }

    @Override
    public byte[] getFile(String fileId) {
        return store.getBlob(fileId);
    }
}
