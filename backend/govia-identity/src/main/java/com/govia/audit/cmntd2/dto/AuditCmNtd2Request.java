package com.govia.audit.cmntd2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AuditCmNtd2Request(
        @NotNull UUID engagementId,
        UUID assignedEmployeeId,
        UUID processStepSummaryId,
        @NotBlank @Size(max = 10) String branchCode,
        @NotNull LocalDate transactionDate,
        LocalDate valueDate,
        @NotBlank @Size(max = 20) String postingUser,
        @NotNull BigDecimal entryNumber,
        @Size(max = 3) String currency,
        BigDecimal amount,
        @Size(max = 50) String accountNumber,
        @Size(max = 20) String bookNumber,
        @Size(max = 30) String transactionType,
        @Size(max = 50) String transactionStatus,
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
