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
 * Exposes the cache over HTTP.
 *
 * PUT /cache/{key} stores the request body under that key.
 * GET /cache/{key} returns the stored bytes, or 404 if the key does not exist.
 * HEAD /cache/{key} returns 200 if the key exists, or 404 if it does not.
 *
 * The controller does not know whether the data is stored in memory, S3, or another backend.
 */
@RestController
@RequestMapping("/cache")
public class CacheController {

    private final CacheStorage storage;

    public CacheController(CacheStorage storage) {
        this.storage = storage;
    }

    @Hidden
    @GetMapping(value = "/hello", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> hello() {
        return ResponseEntity.ok("hello");
    }

    @Operation(summary = "Store cache entry")
    @PutMapping("/{*key}")
    public ResponseEntity<Void> put(
            @PathVariable String key,
            @RequestBody byte[] content) {

        storage.put(normalizeKey(key), content);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get cache entry")
    @GetMapping(value = "/{*key}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> get(@PathVariable String key) {
        return storage.get(normalizeKey(key))
                .map(content -> ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(content))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Check whether cache entry exists")
    @RequestMapping(value = "/{*key}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> head(@PathVariable String key) {
        return storage.exists(normalizeKey(key))
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    private String normalizeKey(String key) {
        return key.startsWith("/") ? key.substring(1) : key;
    }
}
