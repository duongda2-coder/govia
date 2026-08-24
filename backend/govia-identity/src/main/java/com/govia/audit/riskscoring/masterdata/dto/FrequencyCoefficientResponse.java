package com.govia.audit.riskscoring.masterdata.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record FrequencyCoefficientResponse(
        UUID id,
        String code,
        Integer fromYear,
        Integer toYear,
        String label,
        BigDecimal value,
        BigDecimal bonusPoint,
        boolean repeat,
        String repeatCount,
        BigDecimal repeatRiskPoint,
        boolean active
) {
}
