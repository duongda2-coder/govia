package com.govia.audit.riskscoring.masterdata.dto;

import com.govia.audit.riskscoring.masterdata.entity.ObjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Group1Request(
        @NotNull ObjectType objectType,
        @NotBlank String code,
        @NotBlank String name,
        BigDecimal weight,
        LocalDate validFrom,
        LocalDate validTo,
        boolean active
) {
}
