package com.anytypeview.core.domain.usecase.system;

import com.anytypeview.core.dto.HealthDTO;
import org.springframework.stereotype.Service;

@Service
public class GetHealthUC {

    public HealthDTO execute() {
        return new HealthDTO("UP", "anytype-view");
    }
}
