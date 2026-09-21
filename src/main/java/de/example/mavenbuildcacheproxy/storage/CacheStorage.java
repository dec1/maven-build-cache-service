package de.example.mavenbuildcacheproxy.storage;

import java.util.Optional;

public interface CacheStorage {

    void put(String key, byte[] content);

    Optional<byte[]> get(String key);
}
