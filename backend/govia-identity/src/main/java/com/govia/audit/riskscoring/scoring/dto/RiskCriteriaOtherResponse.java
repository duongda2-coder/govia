package com.govia.audit.riskscoring.scoring.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RiskCriteriaOtherResponse(
        UUID id,
        UUID auditObjectCategoryId,
        String auditObjectCategoryCode,
        String auditObjectCategoryName,
        String code,
        String name,
        BigDecimal weight,
        UUID groupHoId,
        String groupHoCode,
        String groupHoName,
        UUID riskTypeHoId,
        String riskTypeHoCode,
        String riskTypeHoName,
        boolean active
) {
}
