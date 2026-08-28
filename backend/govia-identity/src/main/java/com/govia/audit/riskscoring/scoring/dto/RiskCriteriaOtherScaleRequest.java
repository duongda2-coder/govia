package com.govia.audit.riskscoring.scoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RiskCriteriaOtherScaleRequest(
        @NotNull UUID auditObjectCategoryId,
        @NotNull UUID criteriaOtherId,
        @NotNull Integer scaleScore,
        @NotBlank String ratingLevel,
        String description,
        boolean active
) {
}
