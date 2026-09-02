package com.govia.audit.planengagement.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AuditEngagementRelatedUnitRequest(@NotNull UUID auditObjectUnitId) {
}
