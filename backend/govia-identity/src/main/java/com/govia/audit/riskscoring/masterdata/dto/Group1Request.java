package com.govia.audit.riskscoring.masterdata.dto;

import com.govia.audit.riskscoring.masterdata.entity.AuditObjectType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record Group1Request(
        @NotNull AuditObjectType auditObjectType,
        @NotNull UUID auditObjectId,
        @NotBlank String code,
        @NotBlank String name,
        BigDecimal weight,
        LocalDate validFrom,
        LocalDate validTo,
        boolean active
) {
}
