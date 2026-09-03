package com.govia.audit.cmntd13.dto;

import java.time.LocalDate;
import java.util.UUID;

public record AuditCmNtd13Response(
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
        LocalDate occurrenceDate,
        String merchantId,
        String merchantAccountNumber,
        String businessRegistrationName,
        String status,
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
