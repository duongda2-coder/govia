package com.govia.audit.riskscoring.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UserAssignmentRequest(
        @NotBlank String username,
        @NotNull UUID criteriaId,
        String branchCode,
        String classification,
        boolean active
) {
}
