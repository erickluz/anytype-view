package com.anytypeview.core.domain.usecase.sync;

import com.anytypeview.core.dto.SyncResultDTO;
import com.anytypeview.core.gateway.AnytypeGateway;
import com.anytypeview.core.gateway.SnapshotGateway;
import com.anytypeview.infra.config.AnytypeProperties;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RunSyncUC {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunSyncUC.class);

    private final AnytypeProperties anytypeProperties;
    private final AnytypeGateway anytypeGateway;
    private final SnapshotGateway snapshotGateway;

    public RunSyncUC(
        AnytypeProperties anytypeProperties,
        AnytypeGateway anytypeGateway,
        SnapshotGateway snapshotGateway
    ) {
        this.anytypeProperties = anytypeProperties;
        this.anytypeGateway = anytypeGateway;
        this.snapshotGateway = snapshotGateway;
    }

    public SyncResultDTO execute() {
        if (anytypeProperties.apiKey() == null || anytypeProperties.apiKey().isBlank()) {
            return new SyncResultDTO(
                "NOT_CONFIGURED",
                "Configure ANYTYPE_API_KEY antes de executar a sincronizacao.",
                OffsetDateTime.now(),
                null,
                null
            );
        }

        var schema = anytypeGateway.validateExpectedSchema();
        if (!schema.valid()) {
            LOGGER.warn(
                "Modelagem Anytype invalida. Tipos ausentes: {}. Propriedades ausentes: {}",
                schema.missingTypes(),
                schema.missingProperties()
            );
            return new SyncResultDTO(
                "SCHEMA_INVALID",
                "Anytype acessado, mas tipos ou propriedades esperadas nao foram encontrados.",
                OffsetDateTime.now(),
                schema,
                null
            );
        }

        var snapshotData = anytypeGateway.loadSnapshotData();
        var snapshot = snapshotGateway.saveDailySnapshot(snapshotData);
        LOGGER.info(
            "Snapshot diario salvo. Espaco: {} ({}). Objetos: {}. Dias com atividade inferida: {}",
            schema.spaceName(),
            schema.spaceId(),
            snapshot.objectCount(),
            snapshot.activityDays()
        );
        return new SyncResultDTO(
            "SNAPSHOT_SAVED",
            "Snapshot diario salvo com dados reais do Anytype.",
            OffsetDateTime.now(),
            schema,
            snapshot
        );
    }
}
