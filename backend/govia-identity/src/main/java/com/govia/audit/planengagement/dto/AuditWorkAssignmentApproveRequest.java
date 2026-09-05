package com.govia.audit.planengagement.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record AuditWorkAssignmentApproveRequest(
        @NotEmpty List<UUID> assignmentIds
) {
}
