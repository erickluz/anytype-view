package com.anytypeview.core.dto;

public record AnytypeObjectSnapshotDTO(
    String anytypeObjectId,
    String anytypeTypeId,
    String anytypeTypeKey,
    String anytypeTypeName,
    String objectName,
    boolean archived,
    String createdDate,
    String lastModifiedDate,
    String relevantPropertiesHash,
    String relevantPropertiesJson
) {
}
