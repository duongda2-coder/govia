package com.govia.audit.riskscoring.scoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RiskCriteriaQuantitativeValueRequest(
        @NotNull UUID criteriaId,
        @NotBlank String branchCode,
        @NotNull Integer year,
        LocalDate entryDate,
        BigDecimal value
) {
}
