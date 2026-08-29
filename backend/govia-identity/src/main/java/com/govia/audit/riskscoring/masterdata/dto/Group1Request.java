package com.govia.audit.riskscoring.masterdata.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record Group1Request(
        @NotNull UUID auditObjectCategoryId,
        @NotBlank String code,
        @NotBlank String name,
        BigDecimal weight,
        String businessLineCode,
        LocalDate validFrom,
        LocalDate validTo,
        boolean active
) {
}
