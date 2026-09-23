package de.example.mavenbuildcacheproxy.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InMemoryCacheStorage} only.
 *
 * <p>These tests exercise the storage implementation directly, without Spring, HTTP, Docker,
 * LocalStack or S3. They retain the Chunk-1 contract for the development/default backend.</p>
 */
class InMemoryCacheStorageTest {

    private final InMemoryCacheStorage storage = new InMemoryCacheStorage();

    /** Verifies that arbitrary binary data can be stored and retrieved unchanged. */
    @Test
    void storesAndRetrievesBinaryContent() {
        byte[] content = {0x00, 0x01, (byte) 0xff, 0x42};

        storage.put("group/artifact/hash/data", content);

        assertThat(storage.get("group/artifact/hash/data"))
                .hasValue(content);
    }

    /** Verifies that a key that has never been stored is represented by Optional.empty(). */
    @Test
    void returnsEmptyForUnknownKey() {
        assertThat(storage.get("missing")).isEmpty();
    }

    /** Verifies that callers cannot mutate stored data indirectly through byte-array references. */
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
