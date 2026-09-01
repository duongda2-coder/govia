package com.govia.audit.cmntd8.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AuditCmNtd8Request(
        @NotBlank @Size(max = 10) String branchCode,
        @NotNull LocalDate transactionDate,
        BigDecimal referenceNumber,
        @NotBlank @Size(max = 20) String postingUser,
        @NotNull BigDecimal entryNumber,
        BigDecimal amount,
        @Size(max = 3) String currency,
        @Size(max = 120) String orderingParty,
        @Size(max = 120) String beneficiaryParty,
        @Size(max = 20) String beneficiaryAccount,
        @Size(max = 50) String sampleReason,
        @Size(max = 120) String auditResult,
        @Size(max = 120) String recommendationType,
        @Size(max = 120) String transactionStaff,
        @Size(max = 120) String controlUser,
        @Size(max = 120) String controlStaff,
        @Size(max = 120) String controlStaffTitle,
        boolean active
) {
}
