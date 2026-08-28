package com.govia.audit.riskscoring.scoring.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RiskAssessmentOtherExpertRankResponse(
        UUID id,
        Integer year,
        String auditObjectCategoryCode,
        String auditObjectCategoryName,
        String auditObjectCode,
        String auditObjectName,
        BigDecimal riskScore,
        String baseRankLabel,
        String reRankLabel,
        String reason,
        LocalDate assessedDate,
        String expertName,
        String finalRankLabel,
        String updatedBy
) {
}
