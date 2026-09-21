package de.example.mavenbuildcacheproxy.http;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CacheControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
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

    @Test
    void getUnknownObjectReturns404() throws Exception {
        mockMvc.perform(get("/cache/does/not/exist"))
                .andExpect(status().isNotFound());
    }
}
