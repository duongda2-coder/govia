package com.govia.audit.riskscoring.scoring.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RiskAssessmentOtherRankingResponse(
        UUID headerId,
        Integer year,
        String auditObjectCategoryCode,
        String auditObjectCategoryName,
        String auditObjectCode,
        String auditObjectName,
        BigDecimal riskScore,
        String rankLabel
) {
}
