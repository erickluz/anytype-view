package com.anytypeview.core.domain.usecase.dashboard;

import com.anytypeview.core.dto.KnowledgeViewDTO;
import com.anytypeview.core.gateway.DashboardGateway;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetKnowledgeViewsUC {

    private final DashboardGateway dashboardGateway;

    public GetKnowledgeViewsUC(DashboardGateway dashboardGateway) {
        this.dashboardGateway = dashboardGateway;
    }

    public KnowledgeViewDTO.CheckpointsDTO checkpoints() {
        return dashboardGateway.latestCheckpoints().orElseGet(() -> new KnowledgeViewDTO.CheckpointsDTO(
            "EMPTY",
            "Nenhum snapshot salvo. Execute uma sincronizacao para consultar os checkpoints.",
            List.of(),
            List.of()
        ));
    }

    public KnowledgeViewDTO.TopicsDTO topics() {
        return dashboardGateway.latestTopics().orElseGet(() -> new KnowledgeViewDTO.TopicsDTO(
            "EMPTY",
            "Nenhum snapshot salvo. Execute uma sincronizacao para consultar os temas.",
            List.of(),
            List.of()
        ));
    }

    public KnowledgeViewDTO.ConceptsDTO concepts() {
        return dashboardGateway.latestConcepts().orElseGet(() -> new KnowledgeViewDTO.ConceptsDTO(
            "EMPTY",
            "Nenhum snapshot salvo. Execute uma sincronizacao para consultar os conceitos.",
            List.of(),
            List.of()
        ));
    }
}
