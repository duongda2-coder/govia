package com.govia.audit.riskscoring.scoring.dto;

import java.util.UUID;

public record RiskAssessmentOtherHeaderResponse(
        UUID id,
        UUID auditObjectCategoryId,
        String auditObjectCategoryCode,
        String auditObjectCategoryName,
        String auditObjectCode,
        String auditObjectName,
        Integer year,
        boolean active
) {
}
