package com.anytypeview.inbound.http;

import com.anytypeview.core.domain.usecase.anytype.GetAnytypeStatusUC;
import com.anytypeview.core.dto.AnytypeStatusDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/anytype")
public class AnytypeController {

    private final GetAnytypeStatusUC getAnytypeStatusUC;

    public AnytypeController(GetAnytypeStatusUC getAnytypeStatusUC) {
        this.getAnytypeStatusUC = getAnytypeStatusUC;
    }

    @GetMapping("/status")
    public AnytypeStatusDTO status() {
        return getAnytypeStatusUC.execute();
    }
}
