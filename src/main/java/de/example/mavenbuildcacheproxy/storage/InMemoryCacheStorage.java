package de.example.mavenbuildcacheproxy.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Stores cache entries in memory inside the running application.
 *
 * Data is lost when the process stops.
 * This implementation is used by default unless S3 storage is selected.
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
