package com.govia.audit.cmtd2.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AuditCmTd2Response(
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
        String customerCode,
        String customerName,
        String disbursementNumber,
        String businessCode,
        String transactionStatus,
        String currency,
        BigDecimal debitAmount,
        BigDecimal creditAmount,
        String accountNumber,
        BigDecimal postingDateDiff,
        String ipcasReviewResult,
        String documentCheckResult,
        boolean active
) {
}
