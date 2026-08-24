package com.govia.audit.riskscoring.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MatrixRequest(
        @NotNull Integer frequencyLevel,
        @NotBlank String frequencyLabel,
        BigDecimal scoreLowSeverity,
        BigDecimal scoreMediumSeverity,
        BigDecimal scoreHighSeverity,
        boolean active
) {
}
