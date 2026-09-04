package com.govia.audit.cmntd3.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record AuditCmNtd3Request(
        @NotNull UUID engagementId,
        UUID assignedEmployeeId,
        UUID processStepSummaryId,
        @NotBlank @Size(max = 10) String branchCode,
        @Size(max = 50) String customerCode,
        @NotBlank @Size(max = 200) String customerName,
        @Size(max = 200) String customerAddress,
        @Size(max = 50) String corebankCustomerCode,
        @Size(max = 3) String currency,
        BigDecimal originalCurrencyBalance,
        BigDecimal convertedBalance,
        @Size(max = 120) String auditResult,
        @Size(max = 120) String recommendationType,
        @Size(max = 120) String transactionStaff,
        @Size(max = 120) String controlUser,
        @Size(max = 120) String controlStaff,
        @Size(max = 120) String controlStaffTitle,
        boolean active
) {
}
