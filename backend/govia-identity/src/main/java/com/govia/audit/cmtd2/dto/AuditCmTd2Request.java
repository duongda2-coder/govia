package com.govia.audit.cmtd2.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AuditCmTd2Request(
        @NotNull UUID engagementId,
        UUID assignedEmployeeId,
        UUID processStepSummaryId,
        @NotBlank @Size(max = 10) String branchCode,
        @NotNull LocalDate transactionDate,
        @NotNull LocalDate valueDate,
        @NotBlank @Size(max = 20) String postingUser,
        BigDecimal entryNumber,
        @Size(max = 50) String customerCode,
        @NotBlank @Size(max = 200) String customerName,
        @Size(max = 20) String disbursementNumber,
        @Size(max = 20) String businessCode,
        @Size(max = 50) String transactionStatus,
        @Size(max = 3) String currency,
        BigDecimal debitAmount,
        BigDecimal creditAmount,
        @Size(max = 10) String accountNumber,
        @Size(max = 120) String ipcasReviewResult,
        @Size(max = 120) String documentCheckResult,
        boolean active
) {
}
