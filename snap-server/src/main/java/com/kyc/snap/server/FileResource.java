package com.kyc.snap.server;

import com.kyc.snap.api.FileService;
import com.kyc.snap.api.StringJson;
import com.kyc.snap.store.Store;

import lombok.Data;

@Data
public class FileResource implements FileService {

    private final Store store;

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
