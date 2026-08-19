package com.anytypeview.core.dto;

import java.util.List;

public record DashboardDTO(
    String mode,
    String note,
    List<ProblemIndicatorDTO> problemIndicators,
    List<SummaryCardDTO> summary,
    List<ActivityPointDTO> activity,
    List<DailyActivityDTO> activityHistory,
    List<TrendPointDTO> conceptTrend,
    List<UnderstandingSliceDTO> understanding,
    List<TopicProgressDTO> topics,
    List<CheckpointDTO> checkpoints
) {
    public record ProblemIndicatorDTO(String label, String value, String context, String tone) {
    }

    public record SummaryCardDTO(String label, String value, String delta, String caption) {
    }

    public record ActivityPointDTO(String label, int value) {
    }

    public record DailyActivityDTO(String date, int value) {
    }

    public record TrendPointDTO(String label, int value) {
    }

    public record UnderstandingSliceDTO(String label, int value, String color) {
    }

    public record TopicProgressDTO(
        String name,
        int concepts,
        int matureConcepts,
        int progressPercent,
        int initiatedConcepts,
        int initiatedPercent,
        int strongConcepts,
        int strongPercent,
        int checkpointCoveredConcepts,
        int checkpointCoveragePercent,
        int lowUnderstanding,
        int daysSinceCheckpoint
    ) {
    }

    public record CheckpointDTO(
        String topic,
        String age,
        String workedAt,
        String perceivedLevel,
        int sellability
    ) {
    }
}
