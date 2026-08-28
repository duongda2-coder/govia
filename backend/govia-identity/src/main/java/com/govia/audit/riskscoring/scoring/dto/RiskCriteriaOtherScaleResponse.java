package com.govia.audit.riskscoring.scoring.dto;

import java.util.UUID;

public record RiskCriteriaOtherScaleResponse(
        UUID id,
        UUID auditObjectCategoryId,
        String auditObjectCategoryCode,
        String auditObjectCategoryName,
        UUID criteriaOtherId,
        String criteriaOtherCode,
        String criteriaOtherName,
        Integer scaleScore,
        String ratingLevel,
        String description,
        boolean active
) {
}
