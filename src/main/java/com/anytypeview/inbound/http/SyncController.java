package com.anytypeview.inbound.http;

import com.anytypeview.core.domain.usecase.sync.RunSyncUC;
import com.anytypeview.core.dto.SyncResultDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final RunSyncUC runSyncUC;

    public SyncController(RunSyncUC runSyncUC) {
        this.runSyncUC = runSyncUC;
    }

    @PostMapping
    public SyncResultDTO sync() {
        return runSyncUC.execute();
    }
}
