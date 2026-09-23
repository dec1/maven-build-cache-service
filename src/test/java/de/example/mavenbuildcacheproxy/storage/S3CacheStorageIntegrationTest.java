package de.example.mavenbuildcacheproxy.storage;

import de.example.mavenbuildcacheproxy.config.S3Properties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
 * Integration tests for {@link S3CacheStorage} using a real LocalStack S3 endpoint in Docker.
 *
 * <p>Testcontainers starts and removes the LocalStack container automatically. LocalStack is not
 * installed separately. The test deliberately pins LocalStack 4.14.0, the final pre-2026.3
 * Community release, so these basic S3 tests do not require a LocalStack account or auth token.</p>
 *
 * <p>The tests create their own bucket and exercise {@link S3CacheStorage} directly. This keeps
 * the storage-adapter integration test separate from the HTTP contract, which is covered by
 * {@code CacheControllerTest}.</p>
 */
@Testcontainers
class S3CacheStorageIntegrationTest {

    /**
     * Pinned deliberately rather than using {@code latest}: newer LocalStack releases require
     * account authentication even for the free tier. Chunk 2 only needs basic S3 PUT/GET support.
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
     * Proves that {@link S3CacheStorage#put(String, byte[])} writes the exact bytes to S3.
     *
     * <p>The assertion reads the object directly through the AWS SDK rather than through
     * {@code S3CacheStorage#get}, so a matching bug in both adapter methods cannot make the test
     * pass.</p>
     */
    @Test
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
     * Proves that an object written through the adapter can be read back byte-for-byte unchanged.
     */
    @Test
    void getRetrievesExactObject() {
        String key = "group/artifact/hash/round-trip";
        byte[] content = {10, 20, 30, 0, (byte) 0xff};

        storage.put(key, content);

        assertThat(storage.get(key)).hasValue(content);
    }

    /**
     * Proves that an S3 missing-object response becomes the storage abstraction's
     * {@code Optional.empty()} result rather than leaking an S3 exception to the caller.
     */
    @Test
    void getReturnsEmptyForMissingObject() {
        assertThat(storage.get("does/not/exist")).isEmpty();
    }
}
