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
 * Tests the HTTP behaviour of {@link CacheController} through Spring MVC.
 *
 * <p>The full Spring application context is created and requests are dispatched through
 * {@link MockMvc}. The tests therefore cover controller routing, request/response handling,
 * Spring wiring and the currently configured {@code CacheStorage} implementation.</p>
 *
 * <p>These are not real network tests: MockMvc does not start a TCP server or send requests
 * through port 8080. They also do not test S3, LocalStack, Docker networking, Maven Build Cache
 * compatibility, authentication, or production deployment.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(TestResultReporter.class)
class CacheControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Verifies the main HTTP contract for an existing cache object:
     *
     * <ol>
     *   <li>PUT accepts arbitrary binary content at a nested cache path and returns 204.</li>
     *   <li>GET of the same path returns 200.</li>
     *   <li>The response is application/octet-stream.</li>
     *   <li>The returned bytes are exactly the bytes that were stored.</li>
     * </ol>
     *
     * <p>This does not verify persistence outside the running Spring test context and does not
     * exercise an S3 backend.</p>
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
     * Verifies that GET maps a missing cache key to HTTP 404.
     *
     * <p>This only checks the HTTP mapping for a missing key; it does not distinguish between
     * different backend failure causes such as S3 errors or connectivity failures.</p>
     */
    @Test
    @DisplayName("HTTP: unknown cache key returns 404")
    void getUnknownObjectReturns404() throws Exception {
        mockMvc.perform(get("/cache/does/not/exist"))
                .andExpect(status().isNotFound());
    }
}
