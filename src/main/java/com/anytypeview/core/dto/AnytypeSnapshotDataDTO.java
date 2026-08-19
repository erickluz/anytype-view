package com.anytypeview.core.dto;

import java.util.List;

public record AnytypeSnapshotDataDTO(
    String spaceId,
    String spaceName,
    List<AnytypeObjectSnapshotDTO> objects
) {
}
