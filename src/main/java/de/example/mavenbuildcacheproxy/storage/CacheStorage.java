package de.example.mavenbuildcacheproxy.storage;

import java.util.Optional;

/**
 * Stores and retrieves cache entries by key.
 *
 * The HTTP layer uses this interface without needing to know which storage implementation is active.
 */
public interface CacheStorage {

    /**
     * Stores the complete byte array under the given key.
     */
    void put(String key, byte[] content);

    /**
     * Returns the stored bytes, or Optional.empty() if the key does not exist.
     */
    Optional<byte[]> get(String key);

    /**
     * Returns whether the key exists without retrieving its contents.
     */
    boolean exists(String key);
}
