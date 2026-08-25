package com.govia.audit.riskscoring.masterdata.dto;

import java.util.UUID;

public record AuditObjectProcessResponse(
        UUID id,
        String segmentCode,
        String code,
        String name,
        String referenceDocument,
        String auditResult,
        String eventNote,
        String incidentNote,
        String reviewResult,
        boolean active
) {
}
