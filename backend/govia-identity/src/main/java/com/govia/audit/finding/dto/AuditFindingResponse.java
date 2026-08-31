package com.govia.audit.finding.dto;

import java.time.LocalDate;
import java.util.UUID;

public record AuditFindingResponse(
        UUID id,
        String branchCode,
        String branchName,
        String title,
        String description,
        String severity,
        String severityName,
        LocalDate detectedDate,
        boolean active
) {
}
