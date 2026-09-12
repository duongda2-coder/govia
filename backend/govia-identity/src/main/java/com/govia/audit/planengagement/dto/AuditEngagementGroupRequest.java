package com.govia.audit.planengagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AuditEngagementGroupRequest(@NotBlank @Size(max = 20) String groupCode, @NotNull UUID leaderEmployeeId) {
}
