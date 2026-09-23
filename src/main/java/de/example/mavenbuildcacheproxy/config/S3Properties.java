package de.example.mavenbuildcacheproxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

/**
 * Holds the S3 settings read from application.properties or environment variables.
 *
 * Bucket and region are required when S3 storage is enabled.
 * Endpoint is optional and is normally only set for LocalStack or another S3-compatible service.
 * Access and secret keys are optional; if they are omitted, the AWS SDK uses its normal credential
 * lookup.
 */
@ConfigurationProperties(prefix = "cache.storage.s3")
public class S3Properties {

    private URI endpoint;
    private String bucket;
    private String region;
    private String accessKey;
    private String secretKey;

    public URI getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(URI endpoint) {
        this.endpoint = endpoint;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}
