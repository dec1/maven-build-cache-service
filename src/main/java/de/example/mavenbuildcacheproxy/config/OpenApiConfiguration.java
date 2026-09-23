package de.example.mavenbuildcacheproxy.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Maven Build Cache S3 Proxy")
                        .version("0.0.1")
                        .description(
                                "HTTP cache service used by the Maven Build Cache Extension. " +
                                        "Cache entries are stored in memory or S3."));
    }
}