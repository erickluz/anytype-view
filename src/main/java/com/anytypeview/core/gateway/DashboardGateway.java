package com.anytypeview.core.gateway;

import com.anytypeview.core.dto.DashboardDTO;
import com.anytypeview.core.dto.KnowledgeViewDTO;
import java.util.Optional;

public interface DashboardGateway {

    Optional<DashboardDTO> latestDashboard();

    Optional<KnowledgeViewDTO.CheckpointsDTO> latestCheckpoints();

    Optional<KnowledgeViewDTO.TopicsDTO> latestTopics();

    Optional<KnowledgeViewDTO.ConceptsDTO> latestConcepts();
}
