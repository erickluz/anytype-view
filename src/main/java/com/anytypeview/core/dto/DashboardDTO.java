package com.anytypeview.core.dto;

import java.util.List;

public record DashboardDTO(
    String mode,
    String note,
    List<SummaryCardDTO> summary,
    List<ActivityPointDTO> activity,
    List<TrendPointDTO> conceptTrend,
    List<UnderstandingSliceDTO> understanding,
    List<TopicProgressDTO> topics,
    List<CheckpointDTO> checkpoints
) {
    public record SummaryCardDTO(String label, String value, String delta, String caption) {
    }

    public record ActivityPointDTO(String label, int value) {
    }

    public record TrendPointDTO(String label, int value) {
    }

    public record UnderstandingSliceDTO(String label, int value, String color) {
    }

    public record TopicProgressDTO(
        String name,
        int concepts,
        int progressPercent,
        int lowUnderstanding,
        int daysSinceCheckpoint
    ) {
    }

    public record CheckpointDTO(
        String topic,
        String age,
        String perceivedLevel,
        int sellability
    ) {
    }
}
