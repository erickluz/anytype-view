package com.anytypeview.core.domain.usecase.anytype;

import com.anytypeview.core.dto.AnytypeStatusDTO;
import com.anytypeview.infra.config.AnytypeProperties;
import org.springframework.stereotype.Service;

@Service
public class GetAnytypeStatusUC {

    private final AnytypeProperties anytypeProperties;

    public GetAnytypeStatusUC(AnytypeProperties anytypeProperties) {
        this.anytypeProperties = anytypeProperties;
    }

    public AnytypeStatusDTO execute() {
        boolean configured = anytypeProperties.apiKey() != null && !anytypeProperties.apiKey().isBlank();
        return new AnytypeStatusDTO(
            anytypeProperties.baseUrl(),
            anytypeProperties.version(),
            anytypeProperties.spaceName(),
            configured
        );
    }
}
