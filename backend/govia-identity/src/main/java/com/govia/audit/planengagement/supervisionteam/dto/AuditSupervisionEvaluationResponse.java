package com.govia.audit.planengagement.supervisionteam.dto;

import java.time.Instant;
import java.util.UUID;

public record AuditSupervisionEvaluationResponse(
        UUID id,
        UUID engagementId,
        UUID employeeId,
        String employeeCode,
        String employeeName,
        String username,
        boolean progress,
        boolean contentAssured,
        boolean qualityAssured,
        String note,
        Instant evaluatedAt
) {
}
