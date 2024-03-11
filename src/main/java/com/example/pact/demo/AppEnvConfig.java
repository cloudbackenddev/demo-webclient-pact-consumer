package com.example.pact.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.config")
public record AppEnvConfig(String baseUrl) {
}
