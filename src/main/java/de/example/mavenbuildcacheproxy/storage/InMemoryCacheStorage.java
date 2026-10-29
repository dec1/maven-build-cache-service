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
 * Selected when cache.storage.type=memory or the property is absent.
 */
@Component
@ConditionalOnProperty(name = "cache.storage.type", havingValue = "memory", matchIfMissing = true)
public class InMemoryCacheStorage implements CacheStorage {

    private final ConcurrentMap<String, byte[]> objects = new ConcurrentHashMap<>();

    /**
     * Stores a copy so later changes to the caller's array cannot change the entry.
     */
    @Override
    public void put(String key, byte[] content) {
        objects.put(key, Arrays.copyOf(content, content.length));
    }

    /**
     * Returns a copy of the stored bytes, or empty if absent; callers cannot mutate stored data.
     */
    @Override
    public Optional<byte[]> get(String key) {
        byte[] content = objects.get(key);
        return content == null
                ? Optional.empty()
                : Optional.of(Arrays.copyOf(content, content.length));
    }

    /**
     * Checks map membership without copying the entry.
     */
    @Override
    public boolean exists(String key) {
        return objects.containsKey(key);
    }
}
