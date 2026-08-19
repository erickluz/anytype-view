package com.anytypeview.core.dto;

import java.util.List;

public final class KnowledgeViewDTO {

    private KnowledgeViewDTO() {
    }

    public record MetricDTO(String label, String value, String context) {
    }

    public record CheckpointsDTO(String mode, String note, List<MetricDTO> metrics, List<CheckpointDTO> items) {
    }

    public record CheckpointDTO(
        String name,
        String topic,
        String workedAt,
        int daysSinceWorked,
        String perceivedLevel,
        int sellability,
        String status,
        boolean hasGaps,
        boolean hasPracticalApplication,
        int connectedConcepts
    ) {
    }

    public record TopicsDTO(String mode, String note, List<MetricDTO> metrics, List<TopicDTO> items) {
    }

    public record TopicDTO(
        String name,
        String type,
        Integer priority,
        int concepts,
        int matureConcepts,
        int maturityPercent,
        int lowUnderstanding,
        int checkpointCount,
        int subtopics,
        int daysSinceCheckpoint
    ) {
    }

    public record ConceptsDTO(String mode, String note, List<MetricDTO> metrics, List<ConceptDTO> items) {
    }

    public record ConceptDTO(
        String name,
        List<String> topics,
        String understanding,
        String verdict,
        Integer priority,
        boolean hasCheckpoint,
        String lastModifiedAt,
        int daysSinceActivity,
        boolean recentlyChanged
    ) {
    }
}
