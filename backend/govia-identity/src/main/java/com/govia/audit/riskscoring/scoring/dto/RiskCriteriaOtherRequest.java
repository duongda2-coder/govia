package com.govia.audit.riskscoring.scoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record RiskCriteriaOtherRequest(
        @NotNull UUID auditObjectCategoryId,
        @NotBlank String code,
        @NotBlank String name,
        BigDecimal weight,
        UUID groupHoId,
        UUID riskTypeHoId,
        boolean active
) {
}
