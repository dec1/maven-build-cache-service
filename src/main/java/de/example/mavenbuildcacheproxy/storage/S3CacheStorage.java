package de.example.mavenbuildcacheproxy.storage;

import de.example.mavenbuildcacheproxy.config.S3Properties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.Optional;

/**
 * Stores cache entries as objects in the configured S3 bucket.
 *
 * Each cache key becomes the object key. S3Configuration supplies the client and endpoint;
 * the bucket must already exist.
 */
@Component
@ConditionalOnProperty(name = "cache.storage.type", havingValue = "s3")
public class S3CacheStorage implements CacheStorage {

    private final S3Client s3Client;
    private final String bucket;

    public S3CacheStorage(S3Client s3Client, S3Properties properties) {
        this.s3Client = s3Client;
        this.bucket = properties.getBucket();
    }

    /**
     * Writes the complete byte array to the object key; S3 failures propagate to the caller.
     */
    @Override
    public void put(String key, byte[] content) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(content));
    }

    /**
     * Downloads the complete object into memory. A missing key or S3 404 returns empty;
     * other S3 failures propagate to the caller.
     */
    @Override
    public Optional<byte[]> get(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        try {
            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
            return Optional.of(response.asByteArray());
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }

    /**
     * Uses HeadObject without downloading the body. A missing key or S3 404 returns false;
     * other S3 failures propagate to the caller.
     */
    @Override
    public boolean exists(String key) {
        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        try {
            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }
}
