package com.govia.audit.planengagement.dto;

import java.util.UUID;

public record AuditEngagementGroupResponse(
        UUID id,
        UUID auditEngagementId,
        String engagementCode,
        String groupCode,
        String groupName,
        UUID leaderEmployeeId,
        String leaderEmployeeCode,
        String leaderEmployeeName,
        String leaderUsername,
        long memberCount,
        long workItemCount
) {
}
