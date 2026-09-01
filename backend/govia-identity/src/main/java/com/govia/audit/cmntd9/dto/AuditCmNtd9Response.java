package com.govia.audit.cmntd9.dto;

import java.time.LocalDate;
import java.util.UUID;

public record AuditCmNtd9Response(
        UUID id,
        String branchCode,
        LocalDate transactionDate,
        String postingUser,
        String customerCode,
        String customerName,
        String idNumber,
        String customerType,
        String transactionContent,
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
