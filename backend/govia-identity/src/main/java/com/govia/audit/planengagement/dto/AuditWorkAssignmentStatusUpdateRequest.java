package com.govia.audit.planengagement.dto;

import com.govia.audit.planengagement.entity.AssignmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AuditWorkAssignmentStatusUpdateRequest(
        @NotNull AssignmentStatus status,
        @Size(max = 2000) String note
) {
}
