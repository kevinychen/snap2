package com.kyc.snap.store;

import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FileStoreTest {

    final FileStore store = new FileStore();

    @Test
    public void storeAndGetBlob() {
        byte[] blob = {1, 2, 3};
        String id = store.storeBlob(blob);
        assertThat(store.getBlob(id)).isEqualTo(blob);
    }

    @Test
    public void storeAndGetObject() {
        Object object = new Struct(1, true, List.of("a", "b"));
        String id = store.storeObject(object);
        assertThat(store.getObject(id, Struct.class)).isEqualTo(object);

        Object newObject = new Struct(2, false, List.of());
        store.updateObject(id, newObject);
        assertThat(store.getObject(id, Struct.class)).isEqualTo(newObject);
    }

    private record Struct(int num, boolean flag, List<String> values) {}
}
