package com.govia.audit.cmntd7.dto;

import java.util.UUID;

public record AuditCmNtd7Response(
        UUID id,
        UUID engagementId,
        String engagementCode,
        UUID assignedEmployeeId,
        String assignedEmployeeCode,
        String assignedUsername,
        UUID processStepSummaryId,
        String processStepSummaryCode,
        String processStepSummaryName,
        String branchCode,
        String constructionCode,
        String constructionName,
        String content,
        String documentType,
        String completenessAssessment,
        String assessment,
        String auditResult,
        boolean active
) {
}
