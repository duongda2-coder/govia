package com.govia.audit.cmntd3.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AuditCmNtd3Response(
        UUID id,
        String branchCode,
        LocalDate transactionDate,
        String customerCode,
        String customerName,
        String customerAddress,
        String accountNumber,
        String currency,
        BigDecimal originalCurrencyBalance,
        BigDecimal convertedBalance,
        String auditResult,
        String recommendationType,
        String transactionStaff,
        String controlUser,
        String controlStaff,
        String controlStaffTitle,
        boolean active
) {
}
