package com.anytypeview.core.gateway;

import com.anytypeview.core.dto.AnytypeSnapshotDataDTO;
import com.anytypeview.core.dto.SnapshotSummaryDTO;

public interface SnapshotGateway {

    SnapshotSummaryDTO saveDailySnapshot(AnytypeSnapshotDataDTO snapshotData);
}
