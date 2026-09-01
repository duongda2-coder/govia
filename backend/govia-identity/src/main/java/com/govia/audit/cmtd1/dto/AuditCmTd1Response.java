package com.govia.audit.cmtd1.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AuditCmTd1Response(
        UUID id,
        String branchCode,
        LocalDate auditDate,
        String customerCode,
        String customerName,
        BigDecimal approvedAmount,
        String loanPurpose,
        String description,
        BigDecimal onBalanceDebt,
        BigDecimal guaranteeBalance,
        BigDecimal riskClassifiedDebt,
        BigDecimal vamcSoldDebt,
        BigDecimal totalCreditBalance,
        String debtGroup,
        String auditScope,
        String auditorCode,
        String sampleReason,
        String note,
        boolean active
) {
}
