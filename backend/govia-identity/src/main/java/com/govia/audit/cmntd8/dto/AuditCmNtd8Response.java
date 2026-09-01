package com.govia.audit.cmntd8.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AuditCmNtd8Response(
        UUID id,
        String branchCode,
        LocalDate transactionDate,
        BigDecimal referenceNumber,
        String postingUser,
        BigDecimal entryNumber,
        BigDecimal amount,
        String currency,
        String orderingParty,
        String beneficiaryParty,
        String beneficiaryAccount,
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
