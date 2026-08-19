package com.anytypeview.infra.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "anytype")
public record AnytypeProperties(
    String baseUrl,
    String version,
    String apiKey,
    String spaceName
) {
}
