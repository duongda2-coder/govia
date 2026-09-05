package com.govia.audit.planengagement.dto;

import com.govia.audit.planengagement.entity.AssignmentApprovalStatus;
import com.govia.audit.planengagement.entity.AssignmentStatus;
import com.govia.audit.workitem.entity.AuditWorkPhase;

import java.time.Instant;
import java.util.UUID;

/** 1 dong man hinh "Quản lý công việc" (CBKT/THKT) - sheet cung ten trong Tạo CKT (1).xlsx. */
public record AuditWorkManagementItemResponse(
        UUID assignmentId,
        UUID engagementId,
        String engagementCode,
        String engagementName,
        String businessSegmentCode,
        UUID workItemId,
        AuditWorkPhase phase,
        String workItemCode,
        String workItemName,
        UUID employeeId,
        String employeeCode,
        String employeeName,
        String employeeUsername,
        AssignmentStatus status,
        String note,
        AssignmentApprovalStatus approvalStatus,
        String approvedBy,
        Instant approvedAt
) {
}
