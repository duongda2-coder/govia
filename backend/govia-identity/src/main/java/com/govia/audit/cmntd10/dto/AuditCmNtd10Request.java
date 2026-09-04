package com.govia.audit.cmntd10.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AuditCmNtd10Request(
        @NotNull UUID engagementId,
        UUID assignedEmployeeId,
        UUID processStepSummaryId,
        @NotBlank @Size(max = 10) String branchCode,
        @NotNull LocalDate issueDate,
        @Size(max = 50) String customerCode,
        @NotBlank @Size(max = 200) String customerName,
        @NotBlank @Size(max = 20) String accountNumber,
        @Size(max = 100) String cardTier,
        @Size(max = 20) String issuingUser,
        BigDecimal issuanceFee,
        @Size(max = 20) String issuanceType,
        @Size(max = 20) String issuanceOccurrence,
        @Size(max = 1000) String sampleReason,
        @Size(max = 120) String auditResult,
        @Size(max = 120) String recommendationType,
        @Size(max = 120) String transactionStaff,
        @Size(max = 120) String controlUser,
        @Size(max = 120) String controlStaff,
        @Size(max = 120) String controlStaffTitle,
        boolean active
) {
}
