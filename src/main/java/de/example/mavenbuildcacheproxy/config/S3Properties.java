package de.example.mavenbuildcacheproxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

/**
 * External configuration for the S3-backed cache storage.
 *
 * <p>The endpoint and static credentials are optional. A custom endpoint is useful for LocalStack;
 * when it is absent, the AWS SDK uses the normal AWS S3 endpoint for the configured region. When
 * static credentials are absent, the AWS SDK default credential provider chain is used.</p>
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
