package de.example.mavenbuildcacheproxy.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * Creates the S3 client when S3 storage is enabled.
 *
 * With backend=aws, the AWS SDK uses the normal Amazon S3 endpoint and normal AWS credentials.
 * With backend=localstack, the configured LocalStack endpoint and dummy credentials are used.
 */
@Configuration
@ConditionalOnProperty(name = "cache.storage.type", havingValue = "s3")
@EnableConfigurationProperties(S3Properties.class)
public class S3Configuration {

    @Bean
    public S3Client s3Client(S3Properties properties) {
        requireText(properties.getRegion(), "cache.storage.s3.region");
        requireText(properties.getBucket(), "cache.storage.s3.bucket");

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.getRegion()));

        if (properties.getBackend() == S3Properties.Backend.LOCALSTACK) {
            if (properties.getLocalstackEndpoint() == null) {
                throw new IllegalStateException(
                        "cache.storage.s3.localstack-endpoint must be configured when cache.storage.s3.backend=localstack");
            }

            builder.endpointOverride(properties.getLocalstackEndpoint())
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("test", "test")));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }

    private static void requireText(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    propertyName + " must be configured when cache.storage.type=s3");
        }
    }
}
