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
 * Builds the AWS SDK S3 client when S3 is selected as the storage backend.
 *
 * <p>There is no LocalStack-specific branch in application logic. LocalStack is represented only
 * by normal S3 configuration: an endpoint override plus test credentials. Omitting the endpoint and
 * credentials makes the same code suitable for real AWS with the SDK default credential chain.</p>
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

        if (properties.getEndpoint() != null) {
            builder.endpointOverride(properties.getEndpoint());
        }

        boolean hasAccessKey = hasText(properties.getAccessKey());
        boolean hasSecretKey = hasText(properties.getSecretKey());

        if (hasAccessKey != hasSecretKey) {
            throw new IllegalStateException(
                    "cache.storage.s3.access-key and cache.storage.s3.secret-key must either both be set or both be omitted");
        }

        if (hasAccessKey) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        return builder.build();
    }

    private static void requireText(String value, String propertyName) {
        if (!hasText(value)) {
            throw new IllegalStateException(propertyName + " must be configured when cache.storage.type=s3");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
