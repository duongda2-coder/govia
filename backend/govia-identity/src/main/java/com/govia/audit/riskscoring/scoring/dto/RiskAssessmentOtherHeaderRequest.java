package com.govia.audit.riskscoring.scoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RiskAssessmentOtherHeaderRequest(
        @NotNull UUID auditObjectCategoryId,
        @NotBlank String auditObjectCode,
        @NotNull Integer year,
        boolean active
) {
}
