package com.openstrata.srs;

import com.openstrata.srs.config.OpenstrataProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

/** ai-srs-service — Skills / Rules / Specs unified management (optional, port 8083). */
@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(OpenstrataProperties.class)
public class SrsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SrsServiceApplication.class, args);
    }
}
