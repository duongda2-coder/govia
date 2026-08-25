package com.govia.audit.riskscoring.masterdata.dto;

import java.time.LocalDate;
import java.util.UUID;

public record AuditObjectUnitResponse(
        UUID id,
        String code,
        String name,
        String unitType,
        UUID auditObjectCategoryId,
        String auditObjectCategoryCode,
        LocalDate establishedDate,
        LocalDate restructureDate,
        String restructureNote,
        Integer totalStaff,
        Integer leaderCount,
        Integer staffCount,
        Integer rankValue,
        UUID defenseLineGroupId,
        String defenseLineGroupCode,
        String operatingRegulation,
        String mainFunction,
        String keyFindings,
        LocalDate infoUpdatedDate,
        boolean active
) {
}
