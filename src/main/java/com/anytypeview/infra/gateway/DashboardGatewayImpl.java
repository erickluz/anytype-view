package com.anytypeview.infra.gateway;

import com.anytypeview.core.dto.DashboardDTO;
import com.anytypeview.core.dto.KnowledgeViewDTO;
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
        List<DashboardDTO.UnderstandingSliceDTO> understanding = understanding(objects);
        Map<String, LocalDate> latestCheckpointByTopic = latestCheckpointByTopic(objects, namesByObjectId);
        List<DashboardDTO.TopicProgressDTO> topics = topics(objects, namesByObjectId, latestCheckpointByTopic);

        return Optional.of(new DashboardDTO(
            "REAL",
            "Dados reais do ultimo snapshot salvo.",
            problemIndicators(objects, understanding),
            summary(objects),
            activity(),
            activityHistory(),
            conceptTrend(),
            understanding,
            topics,
            recentCheckpoints(objects, namesByObjectId)
        ));
    }

    @Override
    public Optional<KnowledgeViewDTO.CheckpointsDTO> latestCheckpoints() {
        Optional<SnapshotContext> context = latestSnapshotContext();
        if (context.isEmpty()) {
            return Optional.empty();
        }

        List<KnowledgeViewDTO.CheckpointDTO> checkpoints = new ArrayList<>();
        for (SnapshotObjectRow object : context.get().objects()) {
            if (!"Checkpoint de Conhecimento".equals(normalizedTypeName(object.anytypeTypeName()))) {
                continue;
            }
            Optional<LocalDate> workedDate = checkpointWorkDate(object);
            int daysSinceWorked = workedDate
                .map(date -> Math.toIntExact(ChronoUnit.DAYS.between(date, context.get().today())))
                .orElse(-1);
            String topic = objectPropertyIds(object, "Tema").stream()
                .findFirst()
                .map(id -> context.get().namesByObjectId().getOrDefault(id, "Tema não identificado"))
                .orElse("Sem tema associado");
            checkpoints.add(new KnowledgeViewDTO.CheckpointDTO(
                object.objectName(),
                topic,
                workedDate.map(date -> date.format(SHORT_DATE)).orElse("-"),
                daysSinceWorked,
                selectPropertyName(object, "Nivel Percebido").orElse("-"),
                numberProperty(object, "Vendabilidade").orElse(0),
                selectPropertyName(object, "Status").orElse("-"),
                hasTextProperty(object, "Lacunas"),
                hasTextProperty(object, "Aplicacao Pratica"),
                objectPropertyIds(object, "Conecta com").size()
            ));
        }

        checkpoints.sort(Comparator.comparingInt(this::checkpointSortValue));
        int gaps = (int) checkpoints.stream().filter(KnowledgeViewDTO.CheckpointDTO::hasGaps).count();
        int averageSellability = checkpoints.isEmpty()
            ? 0
            : Math.round((float) checkpoints.stream().mapToInt(KnowledgeViewDTO.CheckpointDTO::sellability).average().orElse(0));
        String latest = checkpoints.isEmpty() || checkpoints.get(0).daysSinceWorked() < 0
            ? "Sem data"
            : checkpoints.get(0).workedAt();

        return Optional.of(new KnowledgeViewDTO.CheckpointsDTO(
            "REAL",
            "Dados do último snapshot salvo. A atividade é inferida por data de alteração e revisão disponível.",
            List.of(
                new KnowledgeViewDTO.MetricDTO("Checkpoints", String.valueOf(checkpoints.size()), "sessões de consolidação observadas"),
                new KnowledgeViewDTO.MetricDTO("Último trabalho", latest, "checkpoint mais recentemente alterado"),
                new KnowledgeViewDTO.MetricDTO("Com lacunas", String.valueOf(gaps), "registros com lacunas preenchidas"),
                new KnowledgeViewDTO.MetricDTO("Vendabilidade média", averageSellability + "/10", "autoavaliação dos checkpoints")
            ),
            checkpoints
        ));
    }

    @Override
    public Optional<KnowledgeViewDTO.TopicsDTO> latestTopics() {
        Optional<SnapshotContext> context = latestSnapshotContext();
        if (context.isEmpty()) {
            return Optional.empty();
        }

        Map<String, String> topicTypeById = new HashMap<>();
        Map<String, TopicDetailAccumulator> topics = new LinkedHashMap<>();
        for (SnapshotObjectRow object : context.get().objects()) {
            if (!"Tema".equals(normalizedTypeName(object.anytypeTypeName()))) {
                continue;
            }
            String topicType = selectPropertyName(object, "Tipo").orElse("Tema");
            topicTypeById.put(object.anytypeObjectId(), topicType);
            topics.put(object.anytypeObjectId(), new TopicDetailAccumulator(
                object.objectName(),
                topicType,
                numberProperty(object, "Prioridade").orElse(null)
            ));
        }

        for (SnapshotObjectRow object : context.get().objects()) {
            if (!"Conceito".equals(normalizedTypeName(object.anytypeTypeName()))) {
                continue;
            }
            String understanding = selectPropertyName(object, "Entendimento")
                .map(this::normalizeName)
                .orElse("desconhecido");
            boolean hasCheckpoint = !objectPropertyIds(object, "Checkpoint").isEmpty();
            for (String topicId : objectPropertyIds(object, "Categoria")) {
                TopicDetailAccumulator topic = topics.get(topicId);
                if (topic != null) {
                    topic.addConcept(understanding, hasCheckpoint);
                }
            }
        }

        for (SnapshotObjectRow object : context.get().objects()) {
            if (!"Checkpoint de Conhecimento".equals(normalizedTypeName(object.anytypeTypeName()))) {
                continue;
            }
            Optional<LocalDate> workedDate = checkpointWorkDate(object);
            for (String topicId : objectPropertyIds(object, "Tema")) {
                TopicDetailAccumulator topic = topics.get(topicId);
                if (topic != null) {
                    topic.addCheckpoint(workedDate.orElse(null));
                }
            }
        }

        for (SnapshotObjectRow object : context.get().objects()) {
            if (!"Tema".equals(normalizedTypeName(object.anytypeTypeName()))) {
                continue;
            }
            TopicDetailAccumulator topic = topics.get(object.anytypeObjectId());
            if (topic == null) {
                continue;
            }
            int subtopics = (int) objectPropertyIds(object, "Links").stream()
                .filter(id -> normalizeName(topicTypeById.getOrDefault(id, "")).equals("subtema"))
                .count();
            topic.setSubtopics(subtopics);
        }

        List<KnowledgeViewDTO.TopicDTO> items = topics.values().stream()
            .map(topic -> topic.toDTO(context.get().today()))
            .sorted(Comparator
                .comparingInt(KnowledgeViewDTO.TopicDTO::matureConcepts).reversed()
                .thenComparing(Comparator.comparingInt(KnowledgeViewDTO.TopicDTO::maturityPercent).reversed())
                .thenComparing(Comparator.comparingInt(KnowledgeViewDTO.TopicDTO::concepts).reversed())
                .thenComparing(KnowledgeViewDTO.TopicDTO::name))
            .toList();

        int subtopics = (int) items.stream().filter(item -> normalizeName(item.type()).equals("subtema")).count();
        int withProgress = (int) items.stream().filter(item -> item.matureConcepts() > 0).count();
        int withoutCheckpoint = (int) items.stream()
            .filter(item -> item.concepts() > 0 && item.checkpointCount() == 0)
            .count();

        return Optional.of(new KnowledgeViewDTO.TopicsDTO(
            "REAL",
            "Dados do último snapshot salvo. A lista mostra todos os temas e subtemas observados.",
            List.of(
                new KnowledgeViewDTO.MetricDTO("Temas e subtemas", String.valueOf(items.size()), "estrutura atual do mapa"),
                new KnowledgeViewDTO.MetricDTO("Subtemas", String.valueOf(subtopics), "itens marcados como subtema"),
                new KnowledgeViewDTO.MetricDTO("Com progresso", String.valueOf(withProgress), "temas com conceito intermediário ou forte"),
                new KnowledgeViewDTO.MetricDTO("Sem checkpoint", String.valueOf(withoutCheckpoint), "temas com conceitos sem checkpoint associado")
            ),
            items
        ));
    }

    @Override
    public Optional<KnowledgeViewDTO.ConceptsDTO> latestConcepts() {
        Optional<SnapshotContext> context = latestSnapshotContext();
        if (context.isEmpty()) {
            return Optional.empty();
        }

        List<KnowledgeViewDTO.ConceptDTO> concepts = new ArrayList<>();
        LocalDate recentLimit = context.get().today().minusDays(29);
        for (SnapshotObjectRow object : context.get().objects()) {
            if (!"Conceito".equals(normalizedTypeName(object.anytypeTypeName()))) {
                continue;
            }
            Optional<LocalDate> modifiedDate = parseAnytypeDate(object.lastModifiedDate());
            int daysSinceActivity = modifiedDate
                .map(date -> Math.toIntExact(ChronoUnit.DAYS.between(date, context.get().today())))
                .orElse(-1);
            List<String> topics = objectPropertyIds(object, "Categoria").stream()
                .map(id -> context.get().namesByObjectId().getOrDefault(id, "Tema não identificado"))
                .toList();
            concepts.add(new KnowledgeViewDTO.ConceptDTO(
                object.objectName(),
                topics,
                selectPropertyName(object, "Entendimento").orElse("Desconhecido"),
                selectPropertyName(object, "Veredito").orElse("-"),
                numberProperty(object, "Prioridade").orElse(null),
                !objectPropertyIds(object, "Checkpoint").isEmpty(),
                modifiedDate.map(date -> date.format(SHORT_DATE)).orElse("-"),
                daysSinceActivity,
                modifiedDate.map(date -> !date.isBefore(recentLimit)).orElse(false)
            ));
        }

        concepts.sort(Comparator.comparingInt(this::conceptSortValue));
        int lowUnderstanding = (int) concepts.stream()
            .filter(concept -> isLowUnderstanding(concept.understanding()))
            .count();
        int withCheckpoint = (int) concepts.stream().filter(KnowledgeViewDTO.ConceptDTO::hasCheckpoint).count();
        int recentlyChanged = (int) concepts.stream().filter(KnowledgeViewDTO.ConceptDTO::recentlyChanged).count();

        return Optional.of(new KnowledgeViewDTO.ConceptsDTO(
            "REAL",
            "Dados do último snapshot salvo. A atividade é inferida pela última alteração observada.",
            List.of(
                new KnowledgeViewDTO.MetricDTO("Conceitos", String.valueOf(concepts.size()), "unidades de conhecimento observadas"),
                new KnowledgeViewDTO.MetricDTO("Em nível baixo", String.valueOf(lowUnderstanding), "desconhecido ou básico"),
                new KnowledgeViewDTO.MetricDTO("Com checkpoint", String.valueOf(withCheckpoint), "conceitos ligados a uma revisão"),
                new KnowledgeViewDTO.MetricDTO("Alterados em 30 dias", String.valueOf(recentlyChanged), "sinal de atividade e volatilidade")
            ),
            concepts
        ));
    }

    private Optional<SnapshotContext> latestSnapshotContext() {
        String snapshotId = latestSnapshotId();
        if (snapshotId == null) {
            return Optional.empty();
        }
        List<SnapshotObjectRow> objects = snapshotObjects(snapshotId);
        Map<String, String> namesByObjectId = new HashMap<>();
        for (SnapshotObjectRow object : objects) {
            namesByObjectId.put(object.anytypeObjectId(), object.objectName());
        }
        return Optional.of(new SnapshotContext(objects, namesByObjectId, LocalDate.now(DASHBOARD_ZONE)));
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

    private List<DashboardDTO.ProblemIndicatorDTO> problemIndicators(
        List<SnapshotObjectRow> objects,
        List<DashboardDTO.UnderstandingSliceDTO> understanding
    ) {
        LocalDate today = LocalDate.now(DASHBOARD_ZONE);
        int conceptCount = countByType(objects, "Conceito");
        int lowUnderstanding = understanding.stream()
            .filter(item -> List.of("Desconhecido", "Basico").contains(item.label()))
            .mapToInt(DashboardDTO.UnderstandingSliceDTO::value)
            .sum();
        int progressedConcepts = Math.max(conceptCount - lowUnderstanding, 0);
        int progressPercent = conceptCount == 0 ? 0 : Math.round((progressedConcepts * 100f) / conceptCount);

        int activeDaysLast14 = activeDaysSince(today.minusDays(13));
        int changedObjectsLast7 = changedObjectsSince(today.minusDays(6));
        Optional<LocalDate> lastActivity = lastActivityDate();
        long daysWithoutActivity = lastActivity
            .map(date -> ChronoUnit.DAYS.between(date, today))
            .orElse(0L);

        int volatileObjects = recentlyChangedObjects(objects, today.minusDays(29));

        return List.of(
            new DashboardDTO.ProblemIndicatorDTO(
                "Progresso atual",
                progressPercent + "%",
                progressedConcepts + " de " + conceptCount + " conceitos com entendimento intermediario ou forte",
                progressPercent >= 60 ? "success" : progressPercent >= 35 ? "warning" : "danger"
            ),
            new DashboardDTO.ProblemIndicatorDTO(
                "Ritmo recente",
                activeDaysLast14 + "/14 dias",
                changedObjectsLast7 + " objetos alterados nos ultimos 7 dias",
                activeDaysLast14 >= 6 ? "success" : activeDaysLast14 >= 2 ? "warning" : "danger"
            ),
            new DashboardDTO.ProblemIndicatorDTO(
                "Interrupcao atual",
                lastActivity.map(date -> daysWithoutActivity + " dias").orElse("Sem historico"),
                lastActivity.map(date -> "Ultima atividade inferida em " + date.format(SHORT_DATE)).orElse("Nenhuma atividade inferida ainda"),
                daysWithoutActivity <= 3 ? "success" : daysWithoutActivity <= 10 ? "warning" : "danger"
            ),
            new DashboardDTO.ProblemIndicatorDTO(
                "Volatilidade",
                String.valueOf(volatileObjects),
                "conceitos/checkpoints alterados nos ultimos 30 dias",
                volatileObjects <= 15 ? "success" : volatileObjects <= 60 ? "warning" : "danger"
            )
        );
    }

    private int countByType(List<SnapshotObjectRow> objects, String typeName) {
        int count = 0;
        for (SnapshotObjectRow object : objects) {
            if (typeName.equals(normalizedTypeName(object.anytypeTypeName()))) {
                count++;
            }
        }
        return count;
    }

    private int activeDaysSince(LocalDate startDate) {
        Integer count = jdbcTemplate.queryForObject(
            """
            select count(*)
            from activity_day
            where source = ? and activity_date >= ? and object_count > 0
            """,
            Integer.class,
            "LAST_MODIFIED_DATE",
            startDate.toString()
        );
        return count == null ? 0 : count;
    }

    private int changedObjectsSince(LocalDate startDate) {
        Integer count = jdbcTemplate.queryForObject(
            """
            select coalesce(sum(object_count), 0)
            from activity_day
            where source = ? and activity_date >= ?
            """,
            Integer.class,
            "LAST_MODIFIED_DATE",
            startDate.toString()
        );
        return count == null ? 0 : count;
    }

    private Optional<LocalDate> lastActivityDate() {
        String date = jdbcTemplate.query(
            """
            select max(activity_date)
            from activity_day
            where source = ? and object_count > 0
            """,
            resultSet -> resultSet.next() ? resultSet.getString(1) : null,
            "LAST_MODIFIED_DATE"
        );
        if (date == null || date.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(LocalDate.parse(date));
    }

    private int recentlyChangedObjects(List<SnapshotObjectRow> objects, LocalDate startDate) {
        int count = 0;
        for (SnapshotObjectRow object : objects) {
            String typeName = normalizedTypeName(object.anytypeTypeName());
            if (!List.of("Conceito", "Checkpoint de Conhecimento").contains(typeName)) {
                continue;
            }
            Optional<LocalDate> modifiedDate = parseAnytypeDate(object.lastModifiedDate());
            if (modifiedDate.isPresent() && !modifiedDate.get().isBefore(startDate)) {
                count++;
            }
        }
        return count;
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

    private List<DashboardDTO.DailyActivityDTO> activityHistory() {
        LocalDate today = LocalDate.now(DASHBOARD_ZONE);
        LocalDate startDate = today.minusDays(364);
        Map<LocalDate, Integer> counts = new LinkedHashMap<>();
        for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1)) {
            counts.put(date, 0);
        }

        jdbcTemplate.query(
            """
            select activity_date, object_count
            from activity_day
            where source = ? and activity_date between ? and ?
            order by activity_date
            """,
            resultSet -> {
                LocalDate date = LocalDate.parse(resultSet.getString("activity_date"));
                if (counts.containsKey(date)) {
                    counts.put(date, resultSet.getInt("object_count"));
                }
            },
            "LAST_MODIFIED_DATE",
            startDate.toString(),
            today.toString()
        );

        return counts.entrySet().stream()
            .map(entry -> new DashboardDTO.DailyActivityDTO(entry.getKey().toString(), entry.getValue()))
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
        Map<String, String> namesByObjectId,
        Map<String, LocalDate> latestCheckpointByTopic
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

            String understandingValue = selectPropertyName(object, "Entendimento")
                .map(this::normalizeName)
                .orElse("desconhecido");
            boolean hasCheckpoint = !objectPropertyIds(object, "Checkpoint").isEmpty();

            for (String categoryId : categoryIds) {
                String topicName = namesByObjectId.getOrDefault(categoryId, "Sem tema");
                topics.computeIfAbsent(topicName, key -> new TopicAccumulator()).add(understandingValue, hasCheckpoint);
            }
        }

        LocalDate today = LocalDate.now(DASHBOARD_ZONE);
        return topics.entrySet().stream()
            .sorted((left, right) -> {
                DashboardDTO.TopicProgressDTO leftTopic = left.getValue().toDTO(left.getKey(), latestCheckpointByTopic.get(left.getKey()), today);
                DashboardDTO.TopicProgressDTO rightTopic = right.getValue().toDTO(right.getKey(), latestCheckpointByTopic.get(right.getKey()), today);
                int byMature = Integer.compare(right.getValue().mature, left.getValue().mature);
                if (byMature != 0) {
                    return byMature;
                }
                int byMaturityPercent = Integer.compare(rightTopic.progressPercent(), leftTopic.progressPercent());
                if (byMaturityPercent != 0) {
                    return byMaturityPercent;
                }
                int byInitiated = Integer.compare(right.getValue().initiated, left.getValue().initiated);
                if (byInitiated != 0) {
                    return byInitiated;
                }
                return Integer.compare(rightTopic.concepts(), leftTopic.concepts());
            })
            .limit(6)
            .map(entry -> entry.getValue().toDTO(entry.getKey(), latestCheckpointByTopic.get(entry.getKey()), today))
            .toList();
    }

    private Map<String, LocalDate> latestCheckpointByTopic(
        List<SnapshotObjectRow> objects,
        Map<String, String> namesByObjectId
    ) {
        Map<String, LocalDate> datesByTopic = new HashMap<>();
        for (SnapshotObjectRow object : objects) {
            if (!"Checkpoint de Conhecimento".equals(normalizedTypeName(object.anytypeTypeName()))) {
                continue;
            }
            LocalDate checkpointDate = checkpointWorkDate(object).orElse(null);
            if (checkpointDate == null) {
                continue;
            }
            String topic = objectPropertyIds(object, "Tema").stream()
                .findFirst()
                .map(id -> namesByObjectId.getOrDefault(id, object.objectName()))
                .orElse(object.objectName());
            datesByTopic.merge(topic, checkpointDate, (current, candidate) -> candidate.isAfter(current) ? candidate : current);
        }
        return datesByTopic;
    }

    private List<DashboardDTO.CheckpointDTO> recentCheckpoints(
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
            Optional<LocalDate> workedDate = checkpointWorkDate(object);
            long age = workedDate
                .map(date -> ChronoUnit.DAYS.between(date, today))
                .orElse(0L);
            String level = selectPropertyName(object, "Nivel Percebido").orElse("-");
            int sellability = numberProperty(object, "Vendabilidade").orElse(0);

            checkpoints.add(new DashboardDTO.CheckpointDTO(
                topic,
                age + " dias",
                workedDate.map(date -> date.format(SHORT_DATE)).orElse("-"),
                level,
                sellability
            ));
        }

        return checkpoints.stream()
            .sorted(Comparator.comparingInt((DashboardDTO.CheckpointDTO checkpoint) -> parseDays(checkpoint.age())))
            .limit(3)
            .toList();
    }

    private Optional<LocalDate> checkpointWorkDate(SnapshotObjectRow object) {
        return parseAnytypeDate(object.lastModifiedDate()).or(() -> dateProperty(object, "Ultima Revisao"));
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
            return parseAnytypeDate(value);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private Optional<LocalDate> parseAnytypeDate(String value) {
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

    private boolean hasTextProperty(SnapshotObjectRow object, String propertyName) {
        JsonNode property = property(object, propertyName);
        return property != null && !property.path("text").asText("").isBlank();
    }

    private boolean isLowUnderstanding(String value) {
        String normalized = normalizeName(value);
        return normalized.contains("desconhecido") || normalized.contains("basico");
    }

    private int checkpointSortValue(KnowledgeViewDTO.CheckpointDTO checkpoint) {
        return checkpoint.daysSinceWorked() < 0 ? Integer.MAX_VALUE : checkpoint.daysSinceWorked();
    }

    private int conceptSortValue(KnowledgeViewDTO.ConceptDTO concept) {
        return concept.daysSinceActivity() < 0 ? Integer.MAX_VALUE : concept.daysSinceActivity();
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

    private record SnapshotContext(
        List<SnapshotObjectRow> objects,
        Map<String, String> namesByObjectId,
        LocalDate today
    ) {
    }

    private static class TopicDetailAccumulator {
        private final String name;
        private final String type;
        private final Integer priority;
        private int concepts;
        private int matureConcepts;
        private int lowUnderstanding;
        private int checkpointCount;
        private int subtopics;
        private LocalDate latestCheckpoint;

        TopicDetailAccumulator(String name, String type, Integer priority) {
            this.name = name;
            this.type = type;
            this.priority = priority;
        }

        void addConcept(String understanding, boolean hasCheckpoint) {
            concepts++;
            boolean unknown = understanding.contains("desconhecido");
            boolean basic = understanding.contains("basico");
            boolean mature = understanding.contains("intermediario")
                || understanding.contains("forte")
                || understanding.contains("avancado");
            if (mature) {
                matureConcepts++;
            }
            if (unknown || basic) {
                lowUnderstanding++;
            }
        }

        void addCheckpoint(LocalDate workedAt) {
            checkpointCount++;
            if (workedAt != null && (latestCheckpoint == null || workedAt.isAfter(latestCheckpoint))) {
                latestCheckpoint = workedAt;
            }
        }

        void setSubtopics(int subtopics) {
            this.subtopics = subtopics;
        }

        KnowledgeViewDTO.TopicDTO toDTO(LocalDate today) {
            int maturityPercent = concepts == 0 ? 0 : Math.round((matureConcepts * 100f) / concepts);
            int daysSinceCheckpoint = latestCheckpoint == null
                ? -1
                : Math.toIntExact(ChronoUnit.DAYS.between(latestCheckpoint, today));
            return new KnowledgeViewDTO.TopicDTO(
                name,
                type,
                priority,
                concepts,
                matureConcepts,
                maturityPercent,
                lowUnderstanding,
                checkpointCount,
                subtopics,
                daysSinceCheckpoint
            );
        }
    }

    private static class TopicAccumulator {
        private int concepts;
        private int initiated;
        private int mature;
        private int strong;
        private int lowUnderstanding;
        private int checkpointCovered;

        void add(String understandingValue, boolean hasCheckpoint) {
            concepts++;
            boolean unknown = understandingValue.contains("desconhecido");
            boolean basic = understandingValue.contains("basico");
            boolean intermediate = understandingValue.contains("intermediario");
            boolean strongLevel = understandingValue.contains("forte") || understandingValue.contains("avancado");

            if (!unknown) {
                initiated++;
            }
            if (intermediate || strongLevel) {
                mature++;
            }
            if (strongLevel) {
                strong++;
            }
            if (unknown || basic) {
                lowUnderstanding++;
            }
            if (hasCheckpoint) {
                checkpointCovered++;
            }
        }

        DashboardDTO.TopicProgressDTO toDTO(String name, LocalDate latestCheckpointDate, LocalDate today) {
            int maturityPercent = percent(mature);
            int daysSinceCheckpoint = latestCheckpointDate == null
                ? -1
                : Math.toIntExact(ChronoUnit.DAYS.between(latestCheckpointDate, today));
            return new DashboardDTO.TopicProgressDTO(
                name,
                concepts,
                mature,
                maturityPercent,
                initiated,
                percent(initiated),
                strong,
                percent(strong),
                checkpointCovered,
                percent(checkpointCovered),
                lowUnderstanding,
                daysSinceCheckpoint
            );
        }

        private int percent(int value) {
            return concepts == 0 ? 0 : Math.round((value * 100f) / concepts);
        }
    }
}
