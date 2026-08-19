package com.anytypeview.inbound.http;

import com.anytypeview.core.domain.usecase.system.GetHealthUC;
import com.anytypeview.core.dto.HealthDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemController {

    private final GetHealthUC getHealthUC;

    public SystemController(GetHealthUC getHealthUC) {
        this.getHealthUC = getHealthUC;
    }

    @GetMapping("/api/health")
    public HealthDTO health() {
        return getHealthUC.execute();
    }
}
