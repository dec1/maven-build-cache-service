package de.example.mavenbuildcacheproxy.http;

import de.example.mavenbuildcacheproxy.storage.CacheStorage;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cache")
public class CacheController {

    private final CacheStorage storage;

    public CacheController(CacheStorage storage) {
        this.storage = storage;
    }

    @PutMapping("/{*key}")
    public ResponseEntity<Void> put(
            @PathVariable String key,
            @RequestBody byte[] content) {

        storage.put(normalizeKey(key), content);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{*key}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> get(@PathVariable String key) {
        return storage.get(normalizeKey(key))
                .map(content -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(content))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private String normalizeKey(String key) {
        return key.startsWith("/") ? key.substring(1) : key;
    }
}
