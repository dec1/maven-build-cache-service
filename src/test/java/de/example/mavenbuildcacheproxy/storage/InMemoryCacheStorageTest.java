package de.example.mavenbuildcacheproxy.storage;

import de.example.mavenbuildcacheproxy.testsupport.TestResultReporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests only the in-memory storage implementation.
 *
 * No Spring context, controller, HTTP handling, Docker or S3 is involved.
 *
 * These tests prove the storage rules expected by callers:
 * stored bytes are preserved, missing keys return empty, and callers cannot change stored data
 * by modifying byte arrays they previously passed in or received back.
 */
@ExtendWith(TestResultReporter.class)
class InMemoryCacheStorageTest {

    private final InMemoryCacheStorage storage = new InMemoryCacheStorage();

    /**
     * Stores binary data under one key and reads that key back directly from the in-memory map.
     *
     * Passing proves that put/get preserve the bytes exactly.
     */
    @Test
    @DisplayName("Memory storage: stores and retrieves binary content")
    void storesAndRetrievesBinaryContent() {
        byte[] content = {0x00, 0x01, (byte) 0xff, 0x42};

        storage.put("group/artifact/hash/data", content);

        assertThat(storage.get("group/artifact/hash/data"))
                .hasValue(content);
    }

    /**
     * Reads a key that has never been stored.
     *
     * Passing proves that a missing key is represented by Optional.empty().
     */
    @Test
    @DisplayName("Memory storage: unknown key returns empty")
    void returnsEmptyForUnknownKey() {
        assertThat(storage.get("missing")).isEmpty();
    }

    /**
     * Changes both the original input array and an array returned by get().
     *
     * Passing proves that the storage keeps its own copies and cannot be modified indirectly
     * through array references held by callers.
     */
    @Test
    @DisplayName("Memory storage: stored data cannot be mutated through array references")
    void doesNotExposeMutableStoredArray() {
        byte[] original = {1, 2, 3};
        storage.put("key", original);

        original[0] = 99;
        byte[] retrieved = storage.get("key").orElseThrow();
        retrieved[1] = 99;

        assertThat(storage.get("key")).hasValue(new byte[]{1, 2, 3});
    }
}
