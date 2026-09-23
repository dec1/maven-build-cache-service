package de.example.mavenbuildcacheproxy.storage;

import de.example.mavenbuildcacheproxy.testsupport.TestResultReporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link InMemoryCacheStorage} directly.
 *
 * <p>No HTTP or S3 is involved.</p>
 */
@ExtendWith(TestResultReporter.class)
class InMemoryCacheStorageTest {

    private final InMemoryCacheStorage storage = new InMemoryCacheStorage();

    /**
     * Stored bytes can be read back unchanged.
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
     * A missing key returns {@code Optional.empty()}.
     */
    @Test
    @DisplayName("Memory storage: unknown key returns empty")
    void returnsEmptyForUnknownKey() {
        assertThat(storage.get("missing")).isEmpty();
    }

    /**
     * Changing an input or returned byte array must not change the stored value.
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
