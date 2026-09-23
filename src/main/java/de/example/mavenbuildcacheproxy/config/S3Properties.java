package de.example.mavenbuildcacheproxy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Holds the S3 settings used by the application.
 *
 * Bucket and region are required when S3 storage is enabled.
 * Backend selects either Amazon S3 or the local LocalStack service.
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
}
