package de.example.mavenbuildcacheproxy.storage;

import de.example.mavenbuildcacheproxy.testsupport.TestResultReporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InMemoryCacheStorage} only.
 *
 * <p>These tests exercise the storage implementation directly, without Spring, HTTP, Docker,
 * LocalStack or S3. Their purpose is to define the behaviour expected from the in-memory
 * implementation.</p>
 *
 * <p>They do not prove anything about persistence across process restarts, S3 semantics,
 * networking, concurrent access, or Maven Build Cache compatibility.</p>
 */
@ExtendWith(TestResultReporter.class)
class InMemoryCacheStorageTest {

    private final InMemoryCacheStorage storage = new InMemoryCacheStorage();

    /**
     * Verifies that arbitrary binary data can be stored under a key and retrieved unchanged.
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
     * Verifies that a key that has never been stored is represented by Optional.empty().
     */
    @Test
    @DisplayName("Memory storage: unknown key returns empty")
    void returnsEmptyForUnknownKey() {
        assertThat(storage.get("missing")).isEmpty();
    }

    /**
     * Verifies that callers cannot mutate stored data indirectly through byte-array references.
     *
     * <p>The storage must copy data both when accepting it and when returning it. Otherwise a
     * caller could change cached content without performing another put operation.</p>
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
