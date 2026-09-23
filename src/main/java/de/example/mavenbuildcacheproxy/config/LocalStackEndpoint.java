package de.example.mavenbuildcacheproxy.config;

import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * Provides the LocalStack address used when the application runs with the localstack S3 backend.
 *
 * In the local Docker setup, the LocalStack service is reachable as http://localstack:4566.
 * The automated tests use a dedicated localsatck container, which is independent of this setting
 */
@Component
public class LocalStackEndpoint {

    public URI get() {
        return URI.create("http://localstack:4566");
    }
}
