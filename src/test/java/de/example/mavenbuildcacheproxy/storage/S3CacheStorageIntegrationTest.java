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
 * Tests S3CacheStorage against LocalStack S3.
 *
 * Spring creates S3CacheStorage and its S3Client through the same configuration used by the
 * application. The tests call the storage class directly, without the HTTP controller.
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
        /*
         * Spring needs the LocalStack endpoint while creating the application context.
         * Start the container first so getEndpoint() can return its mapped address.
         */
        LOCALSTACK.start();
    }

    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3CacheStorage storage;

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
     * Writes bytes through S3CacheStorage.put(), then reads the object directly with the
     * application's S3Client.
     *
     * Passing proves that put() creates the expected S3 object with exactly the supplied bytes.
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
     * Writes bytes through S3CacheStorage.put() and reads them back through S3CacheStorage.get().
     *
     * Passing proves that an S3 round trip through the storage adapter preserves the bytes exactly.
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
     * Reads an S3 key that does not exist.
     *
     * Passing proves that S3CacheStorage converts S3's missing-object response to Optional.empty()
     * instead of exposing the AWS SDK exception to its caller.
     */
    @Test
    @DisplayName("S3: missing object returns empty")
    void getReturnsEmptyForMissingObject() {
        assertThat(storage.get("does/not/exist")).isEmpty();
    }

    /**
     * Checks one stored S3 object and one missing key without downloading either object's contents.
     *
     * Passing proves that exists() uses S3 object metadata to distinguish present and missing keys.
     */
    @Test
    @DisplayName("S3: existence check distinguishes stored and missing objects")
    void existenceCheckDistinguishesStoredAndMissingObjects() {
        storage.put("present", new byte[]{1});

        assertThat(storage.exists("present")).isTrue();
        assertThat(storage.exists("missing")).isFalse();
    }
}
