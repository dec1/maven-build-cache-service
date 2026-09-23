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
 * Tests the cache HTTP endpoints without starting a real HTTP server.
 *
 * <p>Requests are sent to the controller through Spring's test support.
 * The configured {@code CacheStorage} is used normally.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(TestResultReporter.class)
class CacheControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * PUT stores binary data and GET returns the same bytes.
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
     * GET returns 404 when the cache key does not exist.
     */
    @Test
    @DisplayName("HTTP: unknown cache key returns 404")
    void getUnknownObjectReturns404() throws Exception {
        mockMvc.perform(get("/cache/does/not/exist"))
                .andExpect(status().isNotFound());
    }
}
