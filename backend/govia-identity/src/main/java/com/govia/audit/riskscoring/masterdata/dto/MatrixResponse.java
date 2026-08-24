package com.govia.audit.riskscoring.masterdata.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MatrixResponse(
        UUID id,
        Integer frequencyLevel,
        String frequencyLabel,
        BigDecimal scoreLowSeverity,
        BigDecimal scoreMediumSeverity,
        BigDecimal scoreHighSeverity,
        boolean active
) {
}
