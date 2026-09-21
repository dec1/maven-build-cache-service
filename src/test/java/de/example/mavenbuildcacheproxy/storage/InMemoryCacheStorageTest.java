package de.example.mavenbuildcacheproxy.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryCacheStorageTest {

    private final InMemoryCacheStorage storage = new InMemoryCacheStorage();

    @Test
    void storesAndRetrievesBinaryContent() {
        byte[] content = {0x00, 0x01, (byte) 0xff, 0x42};

        storage.put("group/artifact/hash/data", content);

        assertThat(storage.get("group/artifact/hash/data"))
                .hasValue(content);
    }

    @Test
    void returnsEmptyForUnknownKey() {
        assertThat(storage.get("missing")).isEmpty();
    }

    @Test
    void doesNotExposeMutableStoredArray() {
        byte[] original = {1, 2, 3};
        storage.put("key", original);

        original[0] = 99;
        byte[] retrieved = storage.get("key").orElseThrow();
        retrieved[1] = 99;

        assertThat(storage.get("key")).hasValue(new byte[]{1, 2, 3});
    }
}
