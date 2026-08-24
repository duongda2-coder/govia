package com.govia.audit.riskscoring.masterdata.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record Group2Response(
        UUID id,
        UUID group1Id,
        String group1Code,
        String code,
        String name,
        BigDecimal weight,
        LocalDate validFrom,
        LocalDate validTo,
        boolean active
) {
}
