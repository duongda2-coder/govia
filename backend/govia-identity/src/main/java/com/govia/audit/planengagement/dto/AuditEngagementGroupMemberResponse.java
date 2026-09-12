package com.govia.audit.planengagement.dto;

import java.util.UUID;

public record AuditEngagementGroupMemberResponse(
        UUID id,
        UUID groupId,
        String groupCode,
        String groupName,
        UUID auditEngagementId,
        String engagementCode,
        UUID employeeId,
        String employeeCode,
        String employeeName,
        String department,
        String username,
        UUID leaderEmployeeId,
        String leaderEmployeeName,
        String leaderUsername,
        UUID businessSegment1Id,
        String businessSegment1Code,
        UUID businessSegment2Id,
        String businessSegment2Code,
        UUID businessSegment3Id,
        String businessSegment3Code
) {
}
