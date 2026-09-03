package com.govia.audit.cmntd9.dto;

import java.time.LocalDate;
import java.util.UUID;

public record AuditCmNtd9Response(
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
        LocalDate transactionDate,
        String postingUser,
        String customerCode,
        String customerName,
        String idNumber,
        String customerType,
        String transactionContent,
        String sampleReason,
        String auditResult,
        String recommendationType,
        String transactionStaff,
        String controlUser,
        String controlStaff,
        String controlStaffTitle,
        boolean active
) {
}
