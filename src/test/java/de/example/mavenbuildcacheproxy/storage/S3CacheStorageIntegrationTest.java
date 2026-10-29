package de.example.mavenbuildcacheproxy.storage;

import de.example.mavenbuildcacheproxy.testsupport.TestResultReporter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests S3CacheStorage directly against LocalStack, using Spring to configure the storage and client.
 * The HTTP controller is not involved. This class owns its LocalStack container and test bucket.
 */
@SpringBootTest(properties = {
        "cache.storage.type=s3",
        "cache.storage.s3.backend=localstack",
        "cache.storage.s3.bucket=maven-build-cache-test"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(TestResultReporter.class)
class S3CacheStorageIntegrationTest {

    private static final DockerImageName LOCALSTACK_IMAGE =
            DockerImageName.parse("localstack/localstack:4.14.0");

    private static final String BUCKET = "maven-build-cache-test";

    private static final LocalStackContainer LOCALSTACK = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices("s3");

    static {
        // Start before Spring resolves the dynamic S3 endpoint.
        LOCALSTACK.start();
    }

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3CacheStorage storage;

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
     * Writes through storage and reads with the S3 client to check the object key and bytes.
     */
    @Test
    @DisplayName("S3: PUT stores exact bytes in S3")
    void putStoresExactObjectInS3() {
        String key = "group/artifact/hash/put-object";
        byte[] content = {0x00, 0x01, (byte) 0xff, 0x42};

        storage.put(key, content);

        byte[] stored = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                        .bucket(BUCKET)
                        .key(key)
                        .build())
                .asByteArray();

        assertThat(stored).isEqualTo(content);
    }

    /**
     * Checks that a storage PUT/GET round trip preserves binary bytes.
     */
    @Test
    @DisplayName("S3: GET retrieves exact bytes")
    void getRetrievesExactObject() {
        String key = "group/artifact/hash/round-trip";
        byte[] content = {10, 20, 30, 0, (byte) 0xff};

        storage.put(key, content);

        assertThat(storage.get(key)).hasValue(content);
    }

    /**
     * Checks that a missing S3 object becomes an empty storage result.
     */
    @Test
    @DisplayName("S3: missing object returns empty")
    void getReturnsEmptyForMissingObject() {
        assertThat(storage.get("does/not/exist")).isEmpty();
    }

    /**
     * Checks HeadObject-based existence for one stored object and one missing key.
     */
    @Test
    @DisplayName("S3: existence check distinguishes stored and missing objects")
    void existenceCheckDistinguishesStoredAndMissingObjects() {
        storage.put("present", new byte[]{1});

        assertThat(storage.exists("present")).isTrue();
        assertThat(storage.exists("missing")).isFalse();
    }
}
