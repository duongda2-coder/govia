package com.govia.audit.riskscoring.masterdata.dto;

import com.govia.audit.riskscoring.masterdata.entity.AuditInspectionType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AuditObjectInspectionHistoryRequest(
        @NotNull UUID auditObjectUnitId,
        @NotNull AuditInspectionType inspectionType,
        @NotNull Integer year,
        String note
) {
}
