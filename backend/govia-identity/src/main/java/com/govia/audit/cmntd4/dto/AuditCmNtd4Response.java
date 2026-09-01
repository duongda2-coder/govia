package com.govia.audit.cmntd4.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AuditCmNtd4Response(
        UUID id,
        String branchCode,
        BigDecimal referenceNumber,
        LocalDate openDate,
        String customerCode,
        String customerName,
        BigDecimal amount,
        String beneficiary,
        String auditResult,
        String recommendationType,
        String transactionStaff,
        String controlUser,
        String controlStaff,
        String controlStaffTitle,
        boolean active
) {
}
