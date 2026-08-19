package com.anytypeview.core.dto;

import java.util.List;

public record AnytypeSchemaValidationDTO(
    String spaceId,
    String spaceName,
    List<String> foundTypes,
    List<String> missingTypes,
    List<String> foundProperties,
    List<String> missingProperties
) {
    public boolean valid() {
        return missingTypes.isEmpty() && missingProperties.isEmpty();
    }
}
