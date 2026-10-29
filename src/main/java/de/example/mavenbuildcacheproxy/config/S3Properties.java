package de.example.mavenbuildcacheproxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

/**
 * Binds cache.storage.s3 settings for S3Configuration, S3CacheStorage and S3HealthIndicator.
 *
 * The backend defaults to AWS. S3Configuration requires bucket and region, plus an endpoint for LocalStack.
 */
@ConfigurationProperties(prefix = "cache.storage.s3")
public class S3Properties {

    public enum Backend {
        AWS,
        LOCALSTACK
    }

    private Backend backend = Backend.AWS;
    private String bucket;
    private String region;
    private URI localStackEndpoint;

    public Backend getBackend() {
        return backend;
    }

    public void setBackend(Backend backend) {
        this.backend = backend;
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

    public URI getLocalStackEndpoint() {
        return localStackEndpoint;
    }

    public void setLocalStackEndpoint(URI localStackEndpoint) {
        this.localStackEndpoint = localStackEndpoint;
    }
}
