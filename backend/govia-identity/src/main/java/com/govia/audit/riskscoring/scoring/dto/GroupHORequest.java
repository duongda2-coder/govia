package com.govia.audit.riskscoring.scoring.dto;

import jakarta.validation.constraints.NotBlank;

public record GroupHORequest(
        @NotBlank String code,
        @NotBlank String name,
        String note,
        boolean active
) {
}
