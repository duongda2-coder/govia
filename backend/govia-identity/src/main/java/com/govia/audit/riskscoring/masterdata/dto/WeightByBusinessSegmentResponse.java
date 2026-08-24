package com.govia.audit.riskscoring.masterdata.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WeightByBusinessSegmentResponse(
        UUID id,
        String segmentCode,
        BigDecimal qualitativeWeight,
        BigDecimal quantitativeWeight,
        Integer fromYear,
        Integer toYear,
        boolean active
) {
}
