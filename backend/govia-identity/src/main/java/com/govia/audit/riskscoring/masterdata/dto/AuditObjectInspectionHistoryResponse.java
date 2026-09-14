package com.govia.audit.riskscoring.masterdata.dto;

import com.govia.audit.riskscoring.masterdata.entity.AuditInspectionType;

import java.util.UUID;

public record AuditObjectInspectionHistoryResponse(
        UUID id,
        UUID auditObjectUnitId,
        AuditInspectionType inspectionType,
        Integer year,
        String note
) {
}
