package com.govia.audit.cmntd4.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AuditCmNtd4Request(
        @NotNull UUID engagementId,
        UUID assignedEmployeeId,
        UUID processStepSummaryId,
        @NotBlank @Size(max = 10) String branchCode,
        @NotNull BigDecimal referenceNumber,
        @NotNull LocalDate openDate,
        @Size(max = 50) String corebankCustomerCode,
        BigDecimal amount,
        @Size(max = 20) String beneficiary,
        @Size(max = 1000) String sampleReason,
        @Size(max = 120) String auditResult,
        @Size(max = 120) String recommendationType,
        @Size(max = 120) String transactionStaff,
        @Size(max = 120) String controlUser,
        @Size(max = 120) String controlStaff,
        @Size(max = 120) String controlStaffTitle,
        boolean active
) {
}
