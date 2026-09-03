package com.govia.audit.cmntd6.dto;

import java.util.UUID;

public record AuditCmNtd6Response(
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
        String staffCode,
        String staffName,
        String ipcasUser,
        String adUser,
        String securityDevice,
        String sampleReason,
        String sampleCode,
        String auditResult,
        boolean active
) {
}
