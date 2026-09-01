package com.govia.audit.cmntd10.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AuditCmNtd10Response(
        UUID id,
        String branchCode,
        LocalDate issueDate,
        String customerCode,
        String customerName,
        String accountNumber,
        String cardTier,
        String issuingUser,
        BigDecimal issuanceFee,
        String issuanceType,
        String issuanceOccurrence,
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
