package com.govia.audit.riskscoring.masterdata.dto;

import com.govia.audit.riskscoring.masterdata.entity.ObjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CriteriaQuantitativeRequest(
        @NotNull ObjectType objectType,
        @NotNull UUID group1Id,
        UUID group2Id,
        @NotBlank String code,
        @NotBlank String name,
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
