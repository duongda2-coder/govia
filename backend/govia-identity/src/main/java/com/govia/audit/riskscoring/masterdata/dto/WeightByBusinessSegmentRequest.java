package com.govia.audit.riskscoring.masterdata.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record WeightByBusinessSegmentRequest(
        @NotBlank String segmentCode,
        BigDecimal qualitativeWeight,
        BigDecimal quantitativeWeight,
        Integer fromYear,
        Integer toYear,
        boolean active
) {
}
