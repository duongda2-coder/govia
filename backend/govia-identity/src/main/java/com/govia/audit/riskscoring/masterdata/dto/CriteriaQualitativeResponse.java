package com.govia.audit.riskscoring.masterdata.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CriteriaQualitativeResponse(
        UUID id,
        String objectType,
        UUID group1Id,
        String group1Code,
        UUID group2Id,
        String group2Code,
        String code,
        String name,
        BigDecimal weight,
        Integer impactLevel,
        Integer likelihoodLevel,
        boolean includeCurrentYear,
        boolean active
) {
}
