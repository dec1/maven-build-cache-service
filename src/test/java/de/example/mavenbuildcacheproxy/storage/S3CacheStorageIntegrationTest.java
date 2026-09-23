package de.example.mavenbuildcacheproxy.storage;

import de.example.mavenbuildcacheproxy.config.S3Properties;
import de.example.mavenbuildcacheproxy.testsupport.TestResultReporter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the S3 storage implementation against LocalStack S3.
 *
 * Testcontainers starts a real LocalStack container and the test creates a real S3 bucket in it.
 * The S3CacheStorage methods then use the AWS SDK against that bucket.
 *
 * No controller or HTTP endpoint is involved here.
 *
 * These tests therefore prove the storage adapter's S3 behaviour independently of the HTTP layer.
 */
@Testcontainers
@ExtendWith(TestResultReporter.class)
class S3CacheStorageIntegrationTest {

    /**
     * The LocalStack version is fixed so the test environment does not change unexpectedly.
     * Version 4.14.0 also works without a LocalStack account or auth token.
     */
    private static final DockerImageName LOCALSTACK_IMAGE =
            DockerImageName.parse("localstack/localstack:4.14.0");

    private static final String BUCKET = "maven-build-cache-test";

    @Container
    private static final LocalStackContainer LOCALSTACK = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices("s3");

    private static S3Client s3Client;
    private static S3CacheStorage storage;

    @BeforeAll
    static void setUp() {
        s3Client = S3Client.builder()
                .endpointOverride(LOCALSTACK.getEndpoint())
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
                .region(Region.of(LOCALSTACK.getRegion()))
                .build();

        s3Client.createBucket(CreateBucketRequest.builder()
                .bucket(BUCKET)
                .build());

        S3Properties properties = new S3Properties();
        properties.setBucket(BUCKET);
        storage = new S3CacheStorage(s3Client, properties);
    }

    /**
     * Writes bytes through S3CacheStorage.put(), then reads the object directly with the AWS SDK.
     *
     * Reading through the SDK avoids using S3CacheStorage.get() to verify S3CacheStorage.put().
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
}
