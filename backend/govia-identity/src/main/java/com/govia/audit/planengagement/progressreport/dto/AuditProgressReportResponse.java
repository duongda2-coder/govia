package com.govia.audit.planengagement.progressreport.dto;

import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AuditProgressReportResponse(
        UUID id,
        UUID engagementId,
        UUID businessSegmentId,
        String businessSegmentCode,
        UUID reportedEmployeeId,
        String reportedEmployeeCode,
        String reportedEmployeeName,
        int totalFindings,
        int totalTtss,
        int totalMaterialFindings,
        int totalMaterialTtss,
        int totalSamples,
        int completedSamples,
        LocalDate reportDate,
        int reportRound,
        String reportedByUsername,
        String note,
        AssignmentApprovalStatus approvalStatus,
        String approvedBy,
        Instant approvedAt
) {
}
