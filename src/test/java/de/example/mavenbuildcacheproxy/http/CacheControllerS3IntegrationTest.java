package de.example.mavenbuildcacheproxy.http;

import de.example.mavenbuildcacheproxy.testsupport.TestResultReporter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.util.Random;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests binary data through the complete application path from the controller to S3 and back.
 *
 * MockMvc sends requests through Spring without opening a real TCP socket.
 * Everything after that is real: CacheController calls S3CacheStorage, the application's S3 client
 * writes to LocalStack S3, and GET reads the object back through the same path.
 *
 * These tests check that binary data is not changed by HTTP request handling, the storage layer,
 * the AWS SDK, S3 storage, or HTTP response handling.
 */
@SpringBootTest(properties = {
        "cache.storage.type=s3",
        "cache.storage.s3.backend=localstack",
        "cache.storage.s3.bucket=maven-build-cache-http-test"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(TestResultReporter.class)
class CacheControllerS3IntegrationTest {

    private static final DockerImageName LOCALSTACK_IMAGE =
            DockerImageName.parse("localstack/localstack:4.14.0");

    private static final String BUCKET = "maven-build-cache-http-test";

    private static final LocalStackContainer LOCALSTACK = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices("s3");

    static {
        /*
         * Spring needs the LocalStack endpoint while creating the application context.
         * Start the container first so getEndpoint() can return its mapped address.
         */
        LOCALSTACK.start();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private S3Client s3Client;

    /**
     * Supplies the LocalStack values that are dynamic for this test run.
     *
     * The test container chooses its own mapped endpoint, so this does not depend on the
     * LocalStack endpoint configured for normal local application runs.
     */
    @DynamicPropertySource
    static void configureS3(DynamicPropertyRegistry registry) {
        registry.add("cache.storage.s3.region", LOCALSTACK::getRegion);
        registry.add("cache.storage.s3.localstack-endpoint", LOCALSTACK::getEndpoint);
    }

    @BeforeAll
    void createBucket() {
        s3Client.createBucket(CreateBucketRequest.builder()
                .bucket(BUCKET)
                .build());
    }

    @AfterAll
    static void stopLocalStack() {
        LOCALSTACK.stop();
    }

    /**
     * Sends every possible byte value (0x00 through 0xff) through PUT and GET.
     *
     * Passing proves that the complete controller -> S3 -> controller path does not interpret the
     * body as text or change any byte value, including zero bytes and values above 0x7f.
     */
    @Test
    @DisplayName("HTTP + S3: preserves all 256 possible byte values")
    void roundTripPreservesEveryPossibleByteValue() throws Exception {
        byte[] payload = new byte[256];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte) i;
        }

        String path = "/cache/binary/all-byte-values";

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
     * Sends 1 MiB of deterministic pseudo-random bytes through PUT and GET.
     *
     * A fixed random seed makes the payload identical on every test run, so a failure is
     * reproducible. The payload contains arbitrary binary values rather than text.
     *
     * Passing proves that a realistically larger binary cache entry survives the complete
     * controller -> S3 -> controller path byte-for-byte without truncation or modification.
     */
    @Test
    @DisplayName("HTTP + S3: preserves a 1 MiB binary payload byte-for-byte")
    void roundTripPreservesOneMiBOfBinaryData() throws Exception {
        byte[] payload = new byte[1024 * 1024];
        new Random(123456789L).nextBytes(payload);

        String path = "/cache/binary/one-mib";

        mockMvc.perform(put(path)
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(payload))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(payload));
    }
}
