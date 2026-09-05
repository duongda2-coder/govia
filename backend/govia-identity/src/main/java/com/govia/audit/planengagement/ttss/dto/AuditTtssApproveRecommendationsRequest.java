package com.govia.audit.planengagement.ttss.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record AuditTtssApproveRecommendationsRequest(
        @NotEmpty List<UUID> ttssRecordIds
) {
}
