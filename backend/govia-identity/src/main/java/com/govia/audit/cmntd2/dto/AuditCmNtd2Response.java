package com.govia.audit.cmntd2.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AuditCmNtd2Response(
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
        LocalDate valueDate,
        String postingUser,
        BigDecimal entryNumber,
        String currency,
        BigDecimal amount,
        String accountNumber,
        String bookNumber,
        String transactionType,
        String transactionStatus,
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
