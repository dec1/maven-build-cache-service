package de.example.mavenbuildcacheproxy.storage;

import java.util.Optional;

/**
 * Storage abstraction used by the HTTP layer.
 *
 * <p>The controller depends only on this interface; concrete backends such as in-memory storage
 * and S3 remain implementation details.</p>
 */
public interface CacheStorage {

    /**
     * Stores the complete binary value under the supplied cache key.
     *
     * @param key cache object key
     * @param content complete object contents
     */
    void put(String key, byte[] content);

    /**
     * Retrieves the complete binary value for the supplied cache key.
     *
     * @param key cache object key
     * @return stored value, or {@link Optional#empty()} when the key does not exist
     */
    Optional<byte[]> get(String key);
}
