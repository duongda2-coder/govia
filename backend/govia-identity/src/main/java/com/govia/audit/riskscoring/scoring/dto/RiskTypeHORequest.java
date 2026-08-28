package com.govia.audit.riskscoring.scoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record RiskTypeHORequest(
        @NotNull UUID groupHoId,
        @NotBlank String code,
        @NotBlank String name,
        BigDecimal weight,
        boolean active
) {
}
