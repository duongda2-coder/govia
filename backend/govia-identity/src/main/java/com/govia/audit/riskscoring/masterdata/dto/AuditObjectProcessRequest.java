package com.govia.audit.riskscoring.masterdata.dto;

import jakarta.validation.constraints.NotBlank;

public record AuditObjectProcessRequest(
        String segmentCode,
        @NotBlank String code,
        @NotBlank String name,
        String referenceDocument,
        String auditResult,
        String eventNote,
        String incidentNote,
        String reviewResult,
        boolean active
) {
}
