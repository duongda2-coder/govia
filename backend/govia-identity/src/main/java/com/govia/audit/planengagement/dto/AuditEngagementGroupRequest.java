package com.govia.audit.planengagement.dto;

import com.govia.audit.planengagement.entity.AuditEngagementGroupCode;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AuditEngagementGroupRequest(@NotNull AuditEngagementGroupCode groupCode, @NotNull UUID leaderEmployeeId) {
}
