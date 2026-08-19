package com.anytypeview.core.domain.usecase.dashboard;

import com.anytypeview.core.dto.DashboardDTO;
import com.anytypeview.core.gateway.DashboardGateway;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetDashboardPreviewUC {

    private final DashboardGateway dashboardGateway;

    public GetDashboardPreviewUC(DashboardGateway dashboardGateway) {
        this.dashboardGateway = dashboardGateway;
    }

    public DashboardDTO execute() {
        return dashboardGateway.latestDashboard().orElseGet(this::preview);
    }

    private DashboardDTO preview() {
        return new DashboardDTO(
            "PREVIEW",
            "Previa baseada na modelagem documentada. Os dados reais entram apos o snapshot.",
            List.of(
                new DashboardDTO.SummaryCardDTO("Temas", "59", "+4 no mes", "Organizacao do mapa"),
                new DashboardDTO.SummaryCardDTO("Conceitos", "414", "+27 no mes", "Unidades de conhecimento"),
                new DashboardDTO.SummaryCardDTO("Checkpoints", "10", "3 atrasados", "Revisoes estruturadas"),
                new DashboardDTO.SummaryCardDTO("Aplicacoes", "2", "2 em andamento", "Projetos praticos")
            ),
            List.of(
                new DashboardDTO.ActivityPointDTO("Seg", 4),
                new DashboardDTO.ActivityPointDTO("Ter", 9),
                new DashboardDTO.ActivityPointDTO("Qua", 2),
                new DashboardDTO.ActivityPointDTO("Qui", 6),
                new DashboardDTO.ActivityPointDTO("Sex", 12),
                new DashboardDTO.ActivityPointDTO("Sab", 1),
                new DashboardDTO.ActivityPointDTO("Dom", 0)
            ),
            List.of(
                new DashboardDTO.TrendPointDTO("Mar", 288),
                new DashboardDTO.TrendPointDTO("Abr", 315),
                new DashboardDTO.TrendPointDTO("Mai", 342),
                new DashboardDTO.TrendPointDTO("Jun", 371),
                new DashboardDTO.TrendPointDTO("Jul", 392),
                new DashboardDTO.TrendPointDTO("Ago", 414)
            ),
            List.of(
                new DashboardDTO.UnderstandingSliceDTO("Desconhecido", 82, "#d64545"),
                new DashboardDTO.UnderstandingSliceDTO("Basico", 168, "#f59e0b"),
                new DashboardDTO.UnderstandingSliceDTO("Intermediario", 128, "#2563eb"),
                new DashboardDTO.UnderstandingSliceDTO("Forte", 36, "#059669")
            ),
            List.of(
                new DashboardDTO.TopicProgressDTO("Spring", 64, 72, 8, 4),
                new DashboardDTO.TopicProgressDTO("Arquitetura", 58, 65, 12, 9),
                new DashboardDTO.TopicProgressDTO("IA aplicada", 42, 44, 19, 3),
                new DashboardDTO.TopicProgressDTO("Cloud Native", 36, 51, 14, 11),
                new DashboardDTO.TopicProgressDTO("Observabilidade", 31, 68, 5, 6)
            ),
            List.of(
                new DashboardDTO.CheckpointDTO("Arquitetura Reativa", "42 dias", "Intermediario", 8),
                new DashboardDTO.CheckpointDTO("Cloud Native", "35 dias", "Basico", 6),
                new DashboardDTO.CheckpointDTO("System Design", "28 dias", "Intermediario", 7)
            )
        );
    }
}
