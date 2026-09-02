package com.govia.audit.planengagement.dto;

import com.govia.audit.planengagement.entity.AuditEngagementGroupCode;

import java.util.UUID;

public record AuditEngagementGroupResponse(
        UUID id,
        UUID auditEngagementId,
        String engagementCode,
        AuditEngagementGroupCode groupCode,
        String groupName,
        UUID leaderEmployeeId,
        String leaderEmployeeCode,
        String leaderEmployeeName,
        long memberCount,
        long workItemCount
) {
}
