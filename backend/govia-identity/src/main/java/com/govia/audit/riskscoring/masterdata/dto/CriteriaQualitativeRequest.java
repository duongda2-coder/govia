package com.govia.audit.riskscoring.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CriteriaQualitativeRequest(
        @NotNull UUID auditObjectCategoryId,
        @NotNull UUID group1Id,
        UUID group2Id,
        @NotBlank String code,
        @NotBlank String name,
        BigDecimal weight,
        Integer impactLevel,
        Integer likelihoodLevel,
        boolean includeCurrentYear,
        boolean active
) {
}
