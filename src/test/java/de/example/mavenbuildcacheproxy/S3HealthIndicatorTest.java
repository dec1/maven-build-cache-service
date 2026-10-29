package de.example.mavenbuildcacheproxy.health;

import de.example.mavenbuildcacheproxy.config.S3Properties;
import de.example.mavenbuildcacheproxy.testsupport.TestResultReporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests bucket-check status mapping with a mocked S3 client; no Spring context or S3 requests.
 */
@ExtendWith(TestResultReporter.class)
class S3HealthIndicatorTest {

    /**
     * Checks that a successful HeadBucket call produces UP.
     */
    @Test
    @DisplayName("Health: reachable S3 bucket reports UP")
    void reachableBucketReportsUp() {
        S3Client s3Client = mock(S3Client.class);
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
                .thenReturn(null);

        S3HealthIndicator indicator = new S3HealthIndicator(s3Client, properties("cache-bucket"));

        assertThat(indicator.health().getStatus().getCode()).isEqualTo("UP");
    }

    /**
     * Checks that a failed HeadBucket call produces DOWN rather than escaping from health().
     */
    @Test
    @DisplayName("Health: failed S3 bucket check reports DOWN")
    void failedBucketCheckReportsDown() {
        S3Client s3Client = mock(S3Client.class);
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(S3Exception.builder()
                        .statusCode(503)
                        .message("S3 unavailable")
                        .build());

        S3HealthIndicator indicator = new S3HealthIndicator(s3Client, properties("cache-bucket"));

        assertThat(indicator.health().getStatus().getCode()).isEqualTo("DOWN");
    }

    private static S3Properties properties(String bucket) {
        S3Properties properties = new S3Properties();
        properties.setBucket(bucket);
        return properties;
    }
}
