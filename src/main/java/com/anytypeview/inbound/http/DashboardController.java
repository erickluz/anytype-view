package com.anytypeview.inbound.http;

import com.anytypeview.core.domain.usecase.dashboard.GetDashboardPreviewUC;
import com.anytypeview.core.dto.DashboardDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final GetDashboardPreviewUC getDashboardPreviewUC;

    public DashboardController(GetDashboardPreviewUC getDashboardPreviewUC) {
        this.getDashboardPreviewUC = getDashboardPreviewUC;
    }

    @GetMapping("/preview")
    public DashboardDTO preview() {
        return getDashboardPreviewUC.execute();
    }
}
