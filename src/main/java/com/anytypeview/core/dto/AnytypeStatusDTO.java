package com.anytypeview.core.dto;

public record AnytypeStatusDTO(
    String baseUrl,
    String version,
    String spaceName,
    boolean apiKeyConfigured
) {
}
