package com.govia.audit.cmntd11.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AuditCmNtd11Response(
        UUID id,
        String branchCode,
        BigDecimal referenceNumber,
        String customerCode,
        String customerName,
        LocalDate transactionDate,
        String currency,
        BigDecimal amount,
        String sampleReason,
        String auditResult,
        String recommendationType,
        String transactionStaff,
        String controlUser,
        String controlStaff,
        String controlStaffTitle,
        boolean active
) {
}
