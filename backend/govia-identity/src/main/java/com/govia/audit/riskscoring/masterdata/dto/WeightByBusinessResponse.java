package com.govia.audit.riskscoring.masterdata.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WeightByBusinessResponse(
        UUID id,
        String businessCode,
        BigDecimal qualitativeWeight,
        BigDecimal quantitativeWeight,
        Integer fromYear,
        Integer toYear,
        boolean active
) {
}
