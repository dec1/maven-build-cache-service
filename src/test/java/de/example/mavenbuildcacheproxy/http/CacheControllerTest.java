package de.example.mavenbuildcacheproxy.http;

import de.example.mavenbuildcacheproxy.testsupport.TestResultReporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests the controller together with the configured (see application.properties) in-memory storage.
 *
 * MockMvc simulates incoming HTTP requests inside Spring, so no real TCP server or socket is used.
 * The controller, request mapping, request/response bodies, status codes and storage calls are real.
 *
 * These tests therefore prove the application's HTTP behaviour up to the storage boundary,
 * but not real network transport and not S3.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(TestResultReporter.class)
class CacheControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Sends PUT /cache/... with binary data, then GETs the same key.
     *
     * The PUT goes through the real controller into the configured in-memory storage.
     * The GET goes through the same path back out again.
     *
     * Passing proves that the controller accepts binary data, stores it under the requested key,
     * returns 204 for PUT, returns 200 for GET, and preserves the bytes exactly.
     */
    @Test
    @DisplayName("HTTP: PUT then GET preserves binary content")
    void putThenGetRoundTripPreservesBinaryContent() throws Exception {
        byte[] payload = {0x00, 0x01, (byte) 0xff, 0x42};
        String path = "/cache/com/example/demo/hash/build-cache-entry";

        mockMvc.perform(put(path)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(payload))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(payload));
    }

    /**
     * Sends GET /cache/... for a key that is not present in the in-memory storage.
     *
     * Passing proves that an empty storage result is translated by the controller to HTTP 404.
     */
    @Test
    @DisplayName("HTTP: unknown cache key returns 404")
    void getUnknownObjectReturns404() throws Exception {
        mockMvc.perform(get("/cache/does/not/exist"))
                .andExpect(status().isNotFound());

    }
}
