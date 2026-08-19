package com.anytypeview.core.gateway;

import com.anytypeview.core.dto.DashboardDTO;
import java.util.Optional;

public interface DashboardGateway {

    Optional<DashboardDTO> latestDashboard();
}
