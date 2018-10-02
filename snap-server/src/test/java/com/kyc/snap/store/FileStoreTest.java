package com.kyc.snap.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import lombok.Data;

public class FileStoreTest {

    FileStore store = new FileStore();

    @Test
    public void storeAndGetBlob() {
        byte[] blob = {1, 2, 3};
        String id = store.storeBlob(blob);
        assertThat(store.getBlob(id)).isEqualTo(blob);
    }

    @Test
    public void storeAndGetObject() {
        Object object = new Struct(1, true, ImmutableList.of("a", "b"));
        String id = store.storeObject(object);
        assertThat(store.getObject(id, Struct.class)).isEqualTo(object);

        Object newObject = new Struct(2, false, ImmutableList.of());
        store.updateObject(id, newObject);
        assertThat(store.getObject(id, Struct.class)).isEqualTo(newObject);
    }

    @Data
    private static class Struct {

        private final int num;
        private final boolean flag;
        private final List<String> values;
    }
}
