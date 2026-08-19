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
                new DashboardDTO.ProblemIndicatorDTO("Progresso atual", "62%", "256 de 414 conceitos acima do nivel baixo", "success"),
                new DashboardDTO.ProblemIndicatorDTO("Ritmo recente", "5/14 dias", "34 objetos alterados nos ultimos 7 dias", "warning"),
                new DashboardDTO.ProblemIndicatorDTO("Interrupcao atual", "2 dias", "Ultima atividade inferida em 17/08", "success"),
                new DashboardDTO.ProblemIndicatorDTO("Volatilidade", "41", "conceitos/checkpoints alterados nos ultimos 30 dias", "warning")
            ),
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
                new DashboardDTO.TopicProgressDTO("Spring", 64, 46, 72, 58, 91, 12, 18, 41, 64, 8, 4),
                new DashboardDTO.TopicProgressDTO("Arquitetura", 58, 38, 65, 49, 84, 8, 14, 34, 59, 12, 9),
                new DashboardDTO.TopicProgressDTO("IA aplicada", 42, 18, 44, 28, 67, 4, 10, 16, 38, 19, 3),
                new DashboardDTO.TopicProgressDTO("Cloud Native", 36, 18, 51, 26, 72, 3, 8, 15, 42, 14, 11),
                new DashboardDTO.TopicProgressDTO("Observabilidade", 31, 21, 68, 27, 87, 6, 19, 19, 61, 5, 6)
            ),
            List.of(
                new DashboardDTO.CheckpointDTO("Arquitetura Reativa", "2 dias", "17/08", "Intermediario", 8),
                new DashboardDTO.CheckpointDTO("Cloud Native", "5 dias", "14/08", "Basico", 6),
                new DashboardDTO.CheckpointDTO("System Design", "8 dias", "11/08", "Intermediario", 7)
            )
        );
    }
}
