package com.govia.audit.cmntd13.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record AuditCmNtd13Request(
        @NotNull UUID engagementId,
        UUID assignedEmployeeId,
        UUID processStepSummaryId,
        @NotBlank @Size(max = 10) String branchCode,
        @NotNull LocalDate occurrenceDate,
        @Size(max = 20) String merchantId,
        @NotBlank @Size(max = 20) String merchantAccountNumber,
        @NotBlank @Size(max = 120) String businessRegistrationName,
        @Size(max = 20) String status,
        @Size(max = 50) String sampleReason,
        @Size(max = 200) String auditResult,
        @Size(max = 120) String recommendationType,
        @Size(max = 120) String transactionStaff,
        @Size(max = 120) String controlUser,
        @Size(max = 120) String controlStaff,
        @Size(max = 120) String controlStaffTitle,
        boolean active
) {
}
