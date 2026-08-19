package com.anytypeview.infra.gateway;

import com.anytypeview.core.dto.AnytypeObjectSnapshotDTO;
import com.anytypeview.core.dto.AnytypeSnapshotDataDTO;
import com.anytypeview.core.dto.SnapshotSummaryDTO;
import com.anytypeview.core.gateway.SnapshotGateway;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SnapshotGatewayImpl implements SnapshotGateway {

    private static final ZoneId SNAPSHOT_ZONE = ZoneId.of("America/Sao_Paulo");

    private final JdbcTemplate jdbcTemplate;

    public SnapshotGatewayImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public SnapshotSummaryDTO saveDailySnapshot(AnytypeSnapshotDataDTO snapshotData) {
        OffsetDateTime now = OffsetDateTime.now(SNAPSHOT_ZONE);
        LocalDate snapshotDate = now.toLocalDate();
        String syncRunId = UUID.randomUUID().toString();
        String snapshotId = UUID.randomUUID().toString();

        jdbcTemplate.update(
            "insert into sync_run (id, started_at, finished_at, status, message) values (?, ?, ?, ?, ?)",
            syncRunId,
            now.toString(),
            now.toString(),
            "SUCCESS",
            "Snapshot diario salvo"
        );

        deleteExistingSnapshot(snapshotDate);

        jdbcTemplate.update(
            """
            insert into daily_snapshot (id, snapshot_date, source, sync_run_id, created_at, object_count, checksum)
            values (?, ?, ?, ?, ?, ?, ?)
            """,
            snapshotId,
            snapshotDate.toString(),
            "REAL",
            syncRunId,
            now.toString(),
            snapshotData.objects().size(),
            snapshotChecksum(snapshotData)
        );

        for (AnytypeObjectSnapshotDTO object : snapshotData.objects()) {
            jdbcTemplate.update(
                """
                insert into snapshot_object (
                    id,
                    daily_snapshot_id,
                    anytype_object_id,
                    anytype_type_id,
                    anytype_type_key,
                    anytype_type_name,
                    object_name,
                    archived,
                    last_modified_date,
                    relevant_properties_hash,
                    relevant_properties_json
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID().toString(),
                snapshotId,
                object.anytypeObjectId(),
                object.anytypeTypeId(),
                object.anytypeTypeKey(),
                object.anytypeTypeName(),
                object.objectName(),
                object.archived() ? 1 : 0,
                object.lastModifiedDate(),
                object.relevantPropertiesHash(),
                object.relevantPropertiesJson()
            );
        }

        int activityDays = saveActivityDays(snapshotData, now);
        return new SnapshotSummaryDTO(
            syncRunId,
            snapshotId,
            snapshotDate,
            snapshotData.objects().size(),
            activityDays
        );
    }

    private void deleteExistingSnapshot(LocalDate snapshotDate) {
        String existingSnapshotId = jdbcTemplate.query(
            "select id from daily_snapshot where snapshot_date = ?",
            resultSet -> resultSet.next() ? resultSet.getString("id") : null,
            snapshotDate.toString()
        );
        if (existingSnapshotId == null) {
            return;
        }

        jdbcTemplate.update("delete from snapshot_object where daily_snapshot_id = ?", existingSnapshotId);
        jdbcTemplate.update("delete from daily_snapshot where id = ?", existingSnapshotId);
    }

    private int saveActivityDays(AnytypeSnapshotDataDTO snapshotData, OffsetDateTime now) {
        Map<LocalDate, Integer> countsByDate = new LinkedHashMap<>();
        for (AnytypeObjectSnapshotDTO object : snapshotData.objects()) {
            LocalDate activityDate = parseDate(object.lastModifiedDate());
            if (activityDate != null) {
                countsByDate.merge(activityDate, 1, Integer::sum);
            }
        }

        for (Map.Entry<LocalDate, Integer> entry : countsByDate.entrySet()) {
            jdbcTemplate.update(
                "delete from activity_day where activity_date = ? and source = ?",
                entry.getKey().toString(),
                "LAST_MODIFIED_DATE"
            );
            jdbcTemplate.update(
                """
                insert into activity_day (id, activity_date, source, object_count, created_at)
                values (?, ?, ?, ?, ?)
                """,
                UUID.randomUUID().toString(),
                entry.getKey().toString(),
                "LAST_MODIFIED_DATE",
                entry.getValue(),
                now.toString()
            );
        }

        return countsByDate.size();
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).atZoneSameInstant(SNAPSHOT_ZONE).toLocalDate();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String snapshotChecksum(AnytypeSnapshotDataDTO snapshotData) {
        return Integer.toHexString(snapshotData.objects().stream()
            .map(AnytypeObjectSnapshotDTO::relevantPropertiesHash)
            .sorted()
            .toList()
            .hashCode());
    }
}
