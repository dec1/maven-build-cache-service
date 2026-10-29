package de.example.mavenbuildcacheproxy.http;

import de.example.mavenbuildcacheproxy.storage.CacheStorage;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes binary cache entries under /cache, delegating storage to CacheStorage.
 *
 * Keys may contain slashes. Request and response bodies are buffered as complete byte arrays.
 */
@RestController
@RequestMapping("/cache")
public class CacheController {

    private final CacheStorage storage;

    public CacheController(CacheStorage storage) {
        this.storage = storage;
    }

    /**
     * Returns a diagnostic greeting at /cache/hello without consulting storage.
     * This GET route takes precedence over retrieving a cache entry named hello.
     */
    @Hidden
    @GetMapping(value = "/hello", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("hello");
    }

    /**
     * Stores or replaces the bytes under the requested key; returns 204 after storage succeeds.
     */
    @Operation(summary = "Store cache entry")
    @PutMapping("/{*key}")
    public ResponseEntity<Void> put(
            @PathVariable String key,
            @RequestBody byte[] content) {

        storage.put(normalizeKey(key), content);
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns stored bytes as application/octet-stream, or 404 for a missing entry.
     */
    @Operation(summary = "Get cache entry")
    @GetMapping(value = "/{*key}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> get(@PathVariable String key) {
        return storage.get(normalizeKey(key))
                .map(content -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(content))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Checks existence and returns 200 or 404 without returning object contents.
     */
    @Operation(summary = "Check whether cache entry exists")
    @RequestMapping(value = "/{*key}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> head(@PathVariable String key) {
        return storage.exists(normalizeKey(key))
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    /**
     * Removes the leading slash included by Spring's {*key} path capture; keeps nested key segments.
     */
    private String normalizeKey(String key) {
        return key.startsWith("/") ? key.substring(1) : key;
    }
}
