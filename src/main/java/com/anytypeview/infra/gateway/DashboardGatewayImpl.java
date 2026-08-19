package com.anytypeview.infra.gateway;

import com.anytypeview.core.dto.DashboardDTO;
import com.anytypeview.core.gateway.DashboardGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DashboardGatewayImpl implements DashboardGateway {

    private static final ZoneId DASHBOARD_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM");

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public DashboardGatewayImpl(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<DashboardDTO> latestDashboard() {
        String snapshotId = latestSnapshotId();
        if (snapshotId == null) {
            return Optional.empty();
        }

        List<SnapshotObjectRow> objects = snapshotObjects(snapshotId);
        Map<String, String> namesByObjectId = new HashMap<>();
        for (SnapshotObjectRow object : objects) {
            namesByObjectId.put(object.anytypeObjectId(), object.objectName());
        }

        return Optional.of(new DashboardDTO(
            "REAL",
            "Dados reais do ultimo snapshot salvo.",
            summary(objects),
            activity(),
            conceptTrend(),
            understanding(objects),
            topics(objects, namesByObjectId),
            checkpoints(objects, namesByObjectId)
        ));
    }

    private String latestSnapshotId() {
        return jdbcTemplate.query(
            "select id from daily_snapshot order by snapshot_date desc limit 1",
            resultSet -> resultSet.next() ? resultSet.getString("id") : null
        );
    }

    private List<SnapshotObjectRow> snapshotObjects(String snapshotId) {
        return jdbcTemplate.query(
            """
            select
                anytype_object_id,
                anytype_type_key,
                anytype_type_name,
                object_name,
                last_modified_date,
                relevant_properties_json
            from snapshot_object
            where daily_snapshot_id = ?
            """,
            (resultSet, rowNum) -> toSnapshotObjectRow(resultSet),
            snapshotId
        );
    }

    private SnapshotObjectRow toSnapshotObjectRow(ResultSet resultSet) throws SQLException {
        return new SnapshotObjectRow(
            resultSet.getString("anytype_object_id"),
            resultSet.getString("anytype_type_key"),
            resultSet.getString("anytype_type_name"),
            resultSet.getString("object_name"),
            resultSet.getString("last_modified_date"),
            resultSet.getString("relevant_properties_json")
        );
    }

    private List<DashboardDTO.SummaryCardDTO> summary(List<SnapshotObjectRow> objects) {
        Map<String, Long> countByType = new LinkedHashMap<>();
        for (String type : List.of("Tema", "Conceito", "Checkpoint de Conhecimento", "Aplicacao")) {
            countByType.put(type, 0L);
        }

        for (SnapshotObjectRow object : objects) {
            String typeName = normalizedTypeName(object.anytypeTypeName());
            countByType.computeIfPresent(typeName, (key, count) -> count + 1);
        }

        return List.of(
            new DashboardDTO.SummaryCardDTO("Temas", String.valueOf(countByType.get("Tema")), "snapshot atual", "Organizacao do mapa"),
            new DashboardDTO.SummaryCardDTO("Conceitos", String.valueOf(countByType.get("Conceito")), "snapshot atual", "Unidades de conhecimento"),
            new DashboardDTO.SummaryCardDTO("Checkpoints", String.valueOf(countByType.get("Checkpoint de Conhecimento")), "snapshot atual", "Revisoes estruturadas"),
            new DashboardDTO.SummaryCardDTO("Aplicacoes", String.valueOf(countByType.get("Aplicacao")), "snapshot atual", "Projetos praticos")
        );
    }

    private List<DashboardDTO.ActivityPointDTO> activity() {
        LocalDate today = LocalDate.now(DASHBOARD_ZONE);
        Map<LocalDate, Integer> counts = new LinkedHashMap<>();
        for (int index = 6; index >= 0; index--) {
            counts.put(today.minusDays(index), 0);
        }

        jdbcTemplate.query(
            """
            select activity_date, object_count
            from activity_day
            where source = ? and activity_date >= ?
            order by activity_date
            """,
            resultSet -> {
                LocalDate date = LocalDate.parse(resultSet.getString("activity_date"));
                if (counts.containsKey(date)) {
                    counts.put(date, resultSet.getInt("object_count"));
                }
            },
            "LAST_MODIFIED_DATE",
            today.minusDays(6).toString()
        );

        return counts.entrySet().stream()
            .map(entry -> new DashboardDTO.ActivityPointDTO(entry.getKey().format(SHORT_DATE), entry.getValue()))
            .toList();
    }

    private List<DashboardDTO.TrendPointDTO> conceptTrend() {
        return jdbcTemplate.query(
            """
            select snapshot_date, object_count
            from daily_snapshot
            order by snapshot_date desc
            limit 6
            """,
            (resultSet, rowNum) -> new DashboardDTO.TrendPointDTO(
                LocalDate.parse(resultSet.getString("snapshot_date")).format(SHORT_DATE),
                resultSet.getInt("object_count")
            )
        ).stream().sorted(Comparator.comparing(DashboardDTO.TrendPointDTO::label)).toList();
    }

    private List<DashboardDTO.UnderstandingSliceDTO> understanding(List<SnapshotObjectRow> objects) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("Desconhecido", 0);
        counts.put("Basico", 0);
        counts.put("Intermediario", 0);
        counts.put("Forte", 0);

        for (SnapshotObjectRow object : objects) {
            if (!"Conceito".equals(normalizedTypeName(object.anytypeTypeName()))) {
                continue;
            }
            String value = selectPropertyName(object, "Entendimento").orElse("Desconhecido");
            String normalized = normalizeName(value);
            if (normalized.contains("intermediario")) {
                counts.merge("Intermediario", 1, Integer::sum);
            } else if (normalized.contains("forte") || normalized.contains("avancado")) {
                counts.merge("Forte", 1, Integer::sum);
            } else if (normalized.contains("basico")) {
                counts.merge("Basico", 1, Integer::sum);
            } else {
                counts.merge("Desconhecido", 1, Integer::sum);
            }
        }

        return List.of(
            new DashboardDTO.UnderstandingSliceDTO("Desconhecido", counts.get("Desconhecido"), "#d64545"),
            new DashboardDTO.UnderstandingSliceDTO("Basico", counts.get("Basico"), "#f59e0b"),
            new DashboardDTO.UnderstandingSliceDTO("Intermediario", counts.get("Intermediario"), "#2563eb"),
            new DashboardDTO.UnderstandingSliceDTO("Forte", counts.get("Forte"), "#059669")
        );
    }

    private List<DashboardDTO.TopicProgressDTO> topics(
        List<SnapshotObjectRow> objects,
        Map<String, String> namesByObjectId
    ) {
        Map<String, TopicAccumulator> topics = new HashMap<>();
        for (SnapshotObjectRow object : objects) {
            if (!"Conceito".equals(normalizedTypeName(object.anytypeTypeName()))) {
                continue;
            }

            List<String> categoryIds = objectPropertyIds(object, "Categoria");
            if (categoryIds.isEmpty()) {
                categoryIds = List.of("sem-tema");
            }

            boolean lowUnderstanding = selectPropertyName(object, "Entendimento")
                .map(this::normalizeName)
                .map(value -> value.contains("desconhecido") || value.contains("basico"))
                .orElse(true);

            for (String categoryId : categoryIds) {
                String topicName = namesByObjectId.getOrDefault(categoryId, "Sem tema");
                topics.computeIfAbsent(topicName, key -> new TopicAccumulator()).add(lowUnderstanding);
            }
        }

        return topics.entrySet().stream()
            .sorted((left, right) -> Integer.compare(right.getValue().concepts, left.getValue().concepts))
            .limit(5)
            .map(entry -> entry.getValue().toDTO(entry.getKey()))
            .toList();
    }

    private List<DashboardDTO.CheckpointDTO> checkpoints(
        List<SnapshotObjectRow> objects,
        Map<String, String> namesByObjectId
    ) {
        List<DashboardDTO.CheckpointDTO> checkpoints = new ArrayList<>();
        LocalDate today = LocalDate.now(DASHBOARD_ZONE);

        for (SnapshotObjectRow object : objects) {
            if (!"Checkpoint de Conhecimento".equals(normalizedTypeName(object.anytypeTypeName()))) {
                continue;
            }

            String topic = objectPropertyIds(object, "Tema").stream()
                .findFirst()
                .map(id -> namesByObjectId.getOrDefault(id, object.objectName()))
                .orElse(object.objectName());
            long age = dateProperty(object, "Ultima Revisao")
                .map(date -> ChronoUnit.DAYS.between(date, today))
                .orElse(0L);
            String level = selectPropertyName(object, "Nivel Percebido").orElse("-");
            int sellability = numberProperty(object, "Vendabilidade").orElse(0);

            checkpoints.add(new DashboardDTO.CheckpointDTO(topic, age + " dias", level, sellability));
        }

        return checkpoints.stream()
            .sorted((left, right) -> Integer.compare(parseDays(right.age()), parseDays(left.age())))
            .limit(5)
            .toList();
    }

    private Optional<String> selectPropertyName(SnapshotObjectRow object, String propertyName) {
        JsonNode property = property(object, propertyName);
        if (property == null) {
            return Optional.empty();
        }
        JsonNode select = property.path("select");
        if (select.isObject()) {
            return Optional.ofNullable(select.path("name").asText(null));
        }
        return Optional.empty();
    }

    private List<String> objectPropertyIds(SnapshotObjectRow object, String propertyName) {
        JsonNode property = property(object, propertyName);
        if (property == null || !property.path("objects").isArray()) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        property.path("objects").forEach(node -> ids.add(node.asText()));
        return ids;
    }

    private Optional<LocalDate> dateProperty(SnapshotObjectRow object, String propertyName) {
        JsonNode property = property(object, propertyName);
        if (property == null) {
            return Optional.empty();
        }
        String value = property.path("date").asText(null);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(OffsetDateTime.parse(value).atZoneSameInstant(DASHBOARD_ZONE).toLocalDate());
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private Optional<Integer> numberProperty(SnapshotObjectRow object, String propertyName) {
        JsonNode property = property(object, propertyName);
        if (property == null || !property.has("number")) {
            return Optional.empty();
        }
        return Optional.of(property.path("number").asInt());
    }

    private JsonNode property(SnapshotObjectRow object, String propertyName) {
        try {
            JsonNode properties = objectMapper.readTree(object.relevantPropertiesJson());
            if (!properties.isArray()) {
                return null;
            }
            for (JsonNode property : properties) {
                String name = property.path("name").asText("");
                String key = property.path("key").asText("");
                if (normalizeName(propertyName).equals(normalizeName(name))
                    || normalizeName(propertyName).equals(normalizeName(key))) {
                    return property;
                }
            }
            return null;
        } catch (Exception exception) {
            return null;
        }
    }

    private String normalizedTypeName(String typeName) {
        String normalized = normalizeName(typeName == null ? "" : typeName);
        if (normalized.equals("aplicacao")) {
            return "Aplicacao";
        }
        if (normalized.equals("checkpoint de conhecimento")) {
            return "Checkpoint de Conhecimento";
        }
        if (normalized.equals("conceito")) {
            return "Conceito";
        }
        if (normalized.equals("tema")) {
            return "Tema";
        }
        return typeName == null ? "" : typeName;
    }

    private String normalizeName(String name) {
        String withoutAccents = Normalizer.normalize(name, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        return withoutAccents
            .trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("\\s+", " ");
    }

    private int parseDays(String value) {
        try {
            return Integer.parseInt(value.split(" ")[0]);
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    private record SnapshotObjectRow(
        String anytypeObjectId,
        String anytypeTypeKey,
        String anytypeTypeName,
        String objectName,
        String lastModifiedDate,
        String relevantPropertiesJson
    ) {
    }

    private static class TopicAccumulator {
        private int concepts;
        private int lowUnderstanding;

        void add(boolean isLowUnderstanding) {
            concepts++;
            if (isLowUnderstanding) {
                lowUnderstanding++;
            }
        }

        DashboardDTO.TopicProgressDTO toDTO(String name) {
            int progress = concepts == 0 ? 0 : Math.round(((concepts - lowUnderstanding) * 100f) / concepts);
            return new DashboardDTO.TopicProgressDTO(name, concepts, progress, lowUnderstanding, 0);
        }
    }
}
