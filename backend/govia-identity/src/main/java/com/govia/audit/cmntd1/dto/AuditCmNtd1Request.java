package com.govia.audit.cmntd1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AuditCmNtd1Request(
        @NotNull UUID engagementId,
        UUID assignedEmployeeId,
        UUID processStepSummaryId,
        @NotBlank @Size(max = 10) String branchCode,
        @NotNull LocalDate transactionDate,
        @NotBlank @Size(max = 20) String postingUser,
        @NotNull BigDecimal entryNumber,
        BigDecimal debitAmount,
        BigDecimal creditAmount,
        @Size(max = 50) String transactionStatus,
        @Size(max = 3) String currency,
        @Size(max = 50) String accountNumber,
        @Size(max = 200) String content,
        @Size(max = 1000) String sampleReason,
        @Size(max = 200) String auditResult,
        @Size(max = 120) String recommendationType,
        @Size(max = 120) String transactionStaff,
        @Size(max = 120) String controlUser,
        @Size(max = 120) String controlStaff,
        @Size(max = 120) String controlStaffTitle,
        @Size(max = 50) String workType,
        boolean active
) {
}
