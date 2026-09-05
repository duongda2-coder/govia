package com.govia.audit.cmntd14.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AuditCmNtd14Response(
        UUID id,
        UUID engagementId,
        String engagementCode,
        UUID assignedEmployeeId,
        String assignedEmployeeCode,
        String assignedUsername,
        UUID processStepSummaryId,
        String processStepSummaryCode,
        String processStepSummaryName,
        String branchCode,
        LocalDate attendanceDate,
        String staffCode,
        String staffName,
        String attendanceCode,
        String description,
        BigDecimal matchedTransactionCount,
        BigDecimal unmatchedTransactionCount,
        BigDecimal adjustedTransactionCount,
        String userCode,
        String note,
        String sampleReason,
        String auditResult,
        boolean active
) {
}
