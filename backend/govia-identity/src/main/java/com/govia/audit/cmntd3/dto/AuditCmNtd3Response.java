package com.govia.audit.cmntd3.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record AuditCmNtd3Response(
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
        String customerCode,
        String customerName,
        String customerAddress,
        String corebankCustomerCode,
        String currency,
        BigDecimal originalCurrencyBalance,
        BigDecimal convertedBalance,
        String auditResult,
        String recommendationType,
        String transactionStaff,
        String controlUser,
        String controlStaff,
        String controlStaffTitle,
        boolean active
) {
}
