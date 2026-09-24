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
 * Tests the S3 part of application health without starting Spring or making a real S3 request.
 *
 * The S3 client is mocked so these tests only verify how a successful or failed bucket check is
 * translated into Actuator health.
 */
@ExtendWith(TestResultReporter.class)
class S3HealthIndicatorTest {

    /**
     * Makes the S3 bucket check succeed.
     *
     * Passing proves that a reachable configured bucket produces health status UP.
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
     * Makes the S3 bucket check fail.
     *
     * Passing proves that an S3 failure produces health status DOWN instead of escaping from the
     * health endpoint as an unhandled exception.
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
