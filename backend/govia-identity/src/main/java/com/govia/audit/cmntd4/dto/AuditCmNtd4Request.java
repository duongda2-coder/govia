package com.govia.audit.cmntd4.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AuditCmNtd4Request(
        @NotBlank @Size(max = 10) String branchCode,
        @NotNull BigDecimal referenceNumber,
        @NotNull LocalDate openDate,
        @Size(max = 50) String customerCode,
        @NotBlank @Size(max = 200) String customerName,
        BigDecimal amount,
        @Size(max = 20) String beneficiary,
        @Size(max = 120) String auditResult,
        @Size(max = 120) String recommendationType,
        @Size(max = 120) String transactionStaff,
        @Size(max = 120) String controlUser,
        @Size(max = 120) String controlStaff,
        @Size(max = 120) String controlStaffTitle,
        boolean active
) {
}
