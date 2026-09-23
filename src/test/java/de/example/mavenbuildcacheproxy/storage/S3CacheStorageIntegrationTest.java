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
 * Tests {@link S3CacheStorage} against a real S3-compatible service.
 *
 * <p>The test starts LocalStack in Docker, creates a bucket, runs the storage operations,
 * then removes the LocalStack container.</p>
 */
@Testcontainers
@ExtendWith(TestResultReporter.class)
class S3CacheStorageIntegrationTest {

    /**
     * Fixed version so the test environment does not change unexpectedly.
     * This version also works without a LocalStack account or auth token.
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
     * PUT writes the exact bytes to S3.
     *
     * <p>The result is read directly with the AWS SDK, not through {@code S3CacheStorage#get()}.</p>
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
     * GET returns exactly the bytes that were stored.
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
     * GET returns {@code Optional.empty()} when the S3 object does not exist.
     */
    @Test
    @DisplayName("S3: missing object returns empty")
    void getReturnsEmptyForMissingObject() {
        assertThat(storage.get("does/not/exist")).isEmpty();
    }
}
