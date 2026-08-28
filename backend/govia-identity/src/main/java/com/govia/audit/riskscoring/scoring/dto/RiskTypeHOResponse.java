package com.govia.audit.riskscoring.scoring.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RiskTypeHOResponse(
        UUID id,
        UUID groupHoId,
        String groupHoCode,
        String groupHoName,
        String code,
        String name,
        BigDecimal weight,
        boolean active
) {
}
