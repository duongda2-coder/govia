package com.govia.audit.cmntd11.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AuditCmNtd11Request(
        @NotBlank @Size(max = 10) String branchCode,
        @NotNull BigDecimal referenceNumber,
        @NotBlank @Size(max = 50) String customerCode,
        @NotBlank @Size(max = 200) String customerName,
        @NotNull LocalDate transactionDate,
        @Size(max = 3) String currency,
        BigDecimal amount,
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
