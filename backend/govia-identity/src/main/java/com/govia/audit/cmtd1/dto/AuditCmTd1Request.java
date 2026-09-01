package com.govia.audit.cmtd1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AuditCmTd1Request(
        @NotBlank @Size(max = 10) String branchCode,
        @NotNull LocalDate auditDate,
        @Size(max = 50) String customerCode,
        @NotBlank @Size(max = 200) String customerName,
        BigDecimal approvedAmount,
        @Size(max = 60) String loanPurpose,
        @Size(max = 120) String description,
        BigDecimal onBalanceDebt,
        BigDecimal guaranteeBalance,
        BigDecimal riskClassifiedDebt,
        BigDecimal vamcSoldDebt,
        @Size(max = 20) String debtGroup,
        @Size(max = 120) String auditScope,
        @Size(max = 50) String auditorCode,
        @Size(max = 50) String sampleReason,
        @Size(max = 120) String note,
        boolean active
) {
}
