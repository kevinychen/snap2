package com.kyc.snap.store;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;

public class FileStore implements Store {

    public static final String BASE_DIR = "./data/store";

    private final File baseDir;
    private final ObjectMapper mapper;

    public FileStore() {
        baseDir = new File(BASE_DIR);
        baseDir.mkdirs();
        mapper = new ObjectMapper();
    }

    @Override
    public String storeBlob(byte[] blob) {
        UUID id = UUID.randomUUID();
        try {
            Files.write(blob, new File(baseDir, id.toString()));
            return id.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] getBlob(String id) {
        try {
            return Files.asByteSource(new File(baseDir, id)).read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String storeObject(Object object) {
        try {
            byte[] blob = mapper.writeValueAsBytes(object);
            return storeBlob(blob);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateObject(String id, Object newObject) {
        try {
            byte[] blob = mapper.writeValueAsBytes(newObject);
            Files.write(blob, new File(baseDir, id));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T getObject(String id, Class<T> clazz) {
        byte[] blob = getBlob(id);
        try {
            return mapper.readValue(blob, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
