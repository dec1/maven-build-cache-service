package de.example.mavenbuildcacheproxy.storage;

import de.example.mavenbuildcacheproxy.testsupport.TestResultReporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests memory storage directly: bytes, missing keys, existence and defensive copies.
 * No Spring context or external services are involved.
 */
@ExtendWith(TestResultReporter.class)
class InMemoryCacheStorageTest {

    private final InMemoryCacheStorage storage = new InMemoryCacheStorage();

    /**
     * Checks that a stored binary value is returned unchanged.
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
     * Checks that an unknown key returns empty.
     */
    @Test
    @DisplayName("Memory storage: unknown key returns empty")
    void returnsEmptyForUnknownKey() {
        assertThat(storage.get("missing")).isEmpty();
    }

    /**
     * Checks existence for one stored key and one missing key.
     */
    @Test
    @DisplayName("Memory storage: existence check distinguishes stored and missing keys")
    void existenceCheckDistinguishesStoredAndMissingKeys() {
        storage.put("present", new byte[]{1});

        assertThat(storage.exists("present")).isTrue();
        assertThat(storage.exists("missing")).isFalse();
    }

    /**
     * Checks that changing the input or a returned array cannot modify the stored entry.
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
