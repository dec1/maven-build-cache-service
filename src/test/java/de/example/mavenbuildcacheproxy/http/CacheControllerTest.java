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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests GET/PUT/HEAD through Spring MockMvc with the default in-memory storage.
 *
 * MockMvc exercises request handling without opening a TCP socket. S3 is not involved.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(TestResultReporter.class)
class CacheControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Checks PUT 204 and GET 200 with the original binary bytes and content type.
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
     * Checks that a missing entry becomes HTTP 404.
     */
    @Test
    @DisplayName("HTTP: unknown cache key returns 404")
    void getUnknownObjectReturns404() throws Exception {
        mockMvc.perform(get("/cache/does/not/exist"))
                .andExpect(status().isNotFound());
    }

    /**
     * Checks that HEAD for a stored entry returns 200 with no body.
     */
    @Test
    @DisplayName("HTTP: HEAD returns 200 for existing cache key")
    void headExistingObjectReturns200() throws Exception {
        String path = "/cache/com/example/demo/hash/head-existing";

        mockMvc.perform(put(path)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(new byte[]{1, 2, 3}))
                .andExpect(status().isNoContent());

        mockMvc.perform(head(path))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[0]));
    }

    /**
     * Checks that HEAD for a missing entry returns 404.
     */
    @Test
    @DisplayName("HTTP: HEAD returns 404 for missing cache key")
    void headUnknownObjectReturns404() throws Exception {
        mockMvc.perform(head("/cache/does/not/exist"))
                .andExpect(status().isNotFound());
    }
}
