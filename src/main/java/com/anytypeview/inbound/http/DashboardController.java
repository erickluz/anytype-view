package com.anytypeview.inbound.http;

import com.anytypeview.core.domain.usecase.dashboard.GetDashboardPreviewUC;
import com.anytypeview.core.domain.usecase.dashboard.GetKnowledgeViewsUC;
import com.anytypeview.core.dto.DashboardDTO;
import com.anytypeview.core.dto.KnowledgeViewDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final GetDashboardPreviewUC getDashboardPreviewUC;
    private final GetKnowledgeViewsUC getKnowledgeViewsUC;

    public DashboardController(GetDashboardPreviewUC getDashboardPreviewUC, GetKnowledgeViewsUC getKnowledgeViewsUC) {
        this.getDashboardPreviewUC = getDashboardPreviewUC;
        this.getKnowledgeViewsUC = getKnowledgeViewsUC;
    }

    @GetMapping("/preview")
    public DashboardDTO preview() {
        return getDashboardPreviewUC.execute();
    }

    @GetMapping("/checkpoints")
    public KnowledgeViewDTO.CheckpointsDTO checkpoints() {
        return getKnowledgeViewsUC.checkpoints();
    }

    @GetMapping("/topics")
    public KnowledgeViewDTO.TopicsDTO topics() {
        return getKnowledgeViewsUC.topics();
    }

    @GetMapping("/concepts")
    public KnowledgeViewDTO.ConceptsDTO concepts() {
        return getKnowledgeViewsUC.concepts();
    }
}
