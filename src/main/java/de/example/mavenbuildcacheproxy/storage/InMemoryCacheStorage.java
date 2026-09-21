package de.example.mavenbuildcacheproxy.storage;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
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
