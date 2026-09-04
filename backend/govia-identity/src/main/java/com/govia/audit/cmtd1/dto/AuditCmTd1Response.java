package com.govia.audit.cmtd1.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AuditCmTd1Response(
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
        LocalDate auditDate,
        String customerCode,
        String sampleFilterUser,
        String customerName,
        BigDecimal approvedAmount,
        String loanPurpose,
        String description,
        BigDecimal onBalanceDebt,
        BigDecimal guaranteeBalance,
        BigDecimal riskClassifiedDebt,
        BigDecimal vamcSoldDebt,
        BigDecimal totalCreditBalance,
        String debtGroup,
        String auditScope,
        String auditorCode,
        String sampleReason,
        String note,
        boolean active
) {
}
