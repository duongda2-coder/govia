package com.govia.audit.riskscoring.scoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RiskCriteriaQualitativeValueRequest(
        @NotNull UUID criteriaId,
        @NotBlank String branchCode,
        @NotNull Integer year,
        String violation,
        String note
) {
}
