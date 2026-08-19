package com.anytypeview.core.gateway;

import com.anytypeview.core.dto.AnytypeSchemaValidationDTO;
import com.anytypeview.core.dto.AnytypeSnapshotDataDTO;

public interface AnytypeGateway {

    AnytypeSchemaValidationDTO validateExpectedSchema();

    AnytypeSnapshotDataDTO loadSnapshotData();
}
