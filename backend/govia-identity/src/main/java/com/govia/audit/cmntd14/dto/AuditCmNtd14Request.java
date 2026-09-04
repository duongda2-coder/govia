package com.govia.audit.cmntd14.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AuditCmNtd14Request(
        @NotNull UUID engagementId,
        UUID assignedEmployeeId,
        UUID processStepSummaryId,
        @NotBlank @Size(max = 10) String branchCode,
        @NotNull LocalDate attendanceDate,
        @NotBlank @Size(max = 20) String staffCode,
        @Size(max = 100) String staffName,
        @Size(max = 1) String attendanceCode,
        @Size(max = 120) String description,
        BigDecimal matchedTransactionCount,
        BigDecimal unmatchedTransactionCount,
        BigDecimal adjustedTransactionCount,
        @Size(max = 15) String userCode,
        @Size(max = 1000) String note,
        @Size(max = 20) String sampleCode,
        @Size(max = 1000) String sampleReason,
        @Size(max = 200) String auditResult,
        boolean active
) {
}
