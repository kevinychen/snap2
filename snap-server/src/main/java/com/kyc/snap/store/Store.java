package com.kyc.snap.store;

public interface Store {

    String storeBlob(byte[] blob);

    byte[] getBlob(String id);

    String storeObject(Object object);

    void updateObject(String id, Object newObject);

    <T> T getObject(String id, Class<T> clazz);
}
