package com.govia.audit.planengagement.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AuditEngagementGroupMemberRequest(
        @NotNull UUID employeeId,
        UUID businessSegment1Id,
        UUID businessSegment2Id,
        UUID businessSegment3Id
) {
}
