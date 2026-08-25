package com.govia.audit.riskscoring.masterdata.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CriteriaQuantitativeResponse(
        UUID id,
        UUID auditObjectCategoryId,
        String auditObjectCategoryCode,
        String auditObjectCategoryName,
        UUID group1Id,
        String group1Code,
        UUID group2Id,
        String group2Code,
        String code,
        String name,
        Integer criteriaType,
        BigDecimal businessThreshold,
        BigDecimal viewThreshold,
        BigDecimal score20,
        BigDecimal score40,
        BigDecimal score60,
        BigDecimal score80,
        BigDecimal score100,
        String scoringGuide,
        boolean includeCurrentYear,
        boolean active
) {
}
