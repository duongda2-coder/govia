package com.govia.audit.riskscoring.masterdata.dto;

import com.govia.audit.riskscoring.masterdata.entity.AuditUnitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record AuditObjectUnitRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotNull AuditUnitType unitType,
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
