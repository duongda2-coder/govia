package com.govia.audit.planengagement.dto;

import com.govia.audit.workitem.entity.AuditWorkPhase;

import java.util.UUID;

public record AuditEngagementAssignmentResponse(
        UUID id,
        UUID groupMemberId,
        UUID groupId,
        String groupName,
        UUID employeeId,
        String employeeCode,
        String employeeName,
        UUID workItemId,
        AuditWorkPhase phase,
        String businessSegmentCode,
        String workItemCode,
        String workItemName
) {
}
