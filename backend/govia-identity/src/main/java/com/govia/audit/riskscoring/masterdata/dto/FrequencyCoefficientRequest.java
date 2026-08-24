package com.govia.audit.riskscoring.masterdata.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record FrequencyCoefficientRequest(
        @NotBlank String code,
        Integer fromYear,
        Integer toYear,
        @NotBlank String label,
        BigDecimal value,
        BigDecimal bonusPoint,
        boolean repeat,
        String repeatCount,
        BigDecimal repeatRiskPoint,
        boolean active
) {
}
