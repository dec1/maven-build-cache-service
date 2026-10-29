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
 * Tests binary PUT/GET through MockMvc, CacheController and S3CacheStorage against LocalStack.
 *
 * Spring configures the application's S3 client; this class owns its LocalStack container and bucket.
 * MockMvc does not open a TCP socket or exercise a Maven client.
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
        // Start before Spring resolves the dynamic S3 endpoint.
        LOCALSTACK.start();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private S3Client s3Client;

    /**
     * Supplies this test container's region and mapped endpoint before Spring creates the S3 client.
     */
    @DynamicPropertySource
    static void configureS3(DynamicPropertyRegistry registry) {
        registry.add("cache.storage.s3.region", LOCALSTACK::getRegion);
        registry.add("cache.storage.s3.localstack-endpoint", LOCALSTACK::getEndpoint);
    }

    /**
     * Creates the bucket before the tests; the application itself does not create buckets.
     */
    @BeforeAll
    void createBucket() {
        s3Client.createBucket(CreateBucketRequest.builder()
                .bucket(BUCKET)
                .build());
    }

    /**
     * Stops the LocalStack container owned by this test class.
     */
    @AfterAll
    static void stopLocalStack() {
        LOCALSTACK.stop();
    }

    /**
     * Checks that all byte values (0x00 through 0xff) survive the controller/S3 round trip.
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
     * Checks a 1 MiB controller/S3 round trip; a fixed random seed makes failures reproducible.
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
