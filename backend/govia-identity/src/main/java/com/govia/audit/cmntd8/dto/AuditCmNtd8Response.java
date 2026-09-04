package com.govia.audit.cmntd8.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AuditCmNtd8Response(
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
        String referenceNumber,
        String postingUser,
        BigDecimal entryNumber,
        BigDecimal amount,
        String currency,
        String orderingParty,
        String beneficiaryParty,
        String beneficiaryAccount,
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
