package com.govia.audit.riskscoring.masterdata.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.UUID;

public record AuditObjectUnitRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String unitType,
        UUID auditObjectCategoryId,
        LocalDate establishedDate,
        LocalDate restructureDate,
        String restructureNote,
        Integer totalStaff,
        Integer leaderCount,
        Integer staffCount,
        Integer rankValue,
        UUID defenseLineGroupId,
        String operatingRegulation,
        String mainFunction,
        String keyFindings,
        boolean active
) {
}
