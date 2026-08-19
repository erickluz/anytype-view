package com.anytypeview.core.dto;

import java.time.LocalDate;

public record SnapshotSummaryDTO(
    String syncRunId,
    String snapshotId,
    LocalDate snapshotDate,
    int objectCount,
    int activityDays
) {
}
