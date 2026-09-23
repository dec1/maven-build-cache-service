package de.example.mavenbuildcacheproxy.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Simple process-local storage used by default for development and the controller tests.
 *
 * <p>It remains available after introducing S3 so Chunk-1 behaviour can still be exercised
 * without Docker or AWS configuration. Set {@code cache.storage.type=s3} to select the S3
 * implementation instead.</p>
 */
@Component
@ConditionalOnProperty(name = "cache.storage.type", havingValue = "memory", matchIfMissing = true)
public class InMemoryCacheStorage implements CacheStorage {

    private final ConcurrentMap<String, byte[]> objects = new ConcurrentHashMap<>();

    @Override
    public void put(String key, byte[] content) {
        objects.put(key, Arrays.copyOf(content, content.length));
    }

    @Override
    public Optional<byte[]> get(String key) {
        byte[] content = objects.get(key);
        return content == null
                ? Optional.empty()
                : Optional.of(Arrays.copyOf(content, content.length));
    }
}
