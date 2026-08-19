package com.anytypeview.core.dto;

import java.time.OffsetDateTime;

public record SyncResultDTO(
    String status,
    String message,
    OffsetDateTime executedAt,
    AnytypeSchemaValidationDTO anytypeSchema,
    SnapshotSummaryDTO snapshot
) {
}
