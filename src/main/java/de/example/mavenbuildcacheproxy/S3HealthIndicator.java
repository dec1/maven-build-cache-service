package de.example.mavenbuildcacheproxy.health;

import de.example.mavenbuildcacheproxy.config.S3Properties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

/**
 * Adds a bucket-access check to /actuator/health when S3 storage is enabled.
 */
@Component
@ConditionalOnProperty(name = "cache.storage.type", havingValue = "s3")
public class S3HealthIndicator extends AbstractHealthIndicator {

    private final S3Client s3Client;
    private final String bucket;

    public S3HealthIndicator(S3Client s3Client, S3Properties properties) {
        this.s3Client = s3Client;
        this.bucket = properties.getBucket();
    }

    /**
     * Marks the bucket UP after HeadBucket succeeds. AbstractHealthIndicator converts failures to DOWN.
     * This checks bucket access, not object read/write permissions.
     */
    @Override
    protected void doHealthCheck(Health.Builder builder) {
        s3Client.headBucket(HeadBucketRequest.builder()
                .bucket(bucket)
                .build());

        builder.up();
    }
}
