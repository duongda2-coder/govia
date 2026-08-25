package com.govia.audit.riskscoring.masterdata.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record Group1Response(
        UUID id,
        UUID auditObjectCategoryId,
        String auditObjectCategoryCode,
        String auditObjectCategoryName,
        String code,
        String name,
        BigDecimal weight,
        LocalDate validFrom,
        LocalDate validTo,
        boolean active
) {
}
