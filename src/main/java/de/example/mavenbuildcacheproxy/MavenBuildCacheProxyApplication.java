package de.example.mavenbuildcacheproxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point; discovers the HTTP controller, storage and configuration beans.
 */
@SpringBootApplication
public class MavenBuildCacheProxyApplication {

    /**
     * Starts the application and HTTP server with configuration from properties and arguments.
     */
    public static void main(String[] args) {
        SpringApplication.run(MavenBuildCacheProxyApplication.class, args);
    }
}
