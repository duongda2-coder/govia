package com.govia.audit.planengagement.ttss.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/** "4. Gắn kiến nghị" - gan 1 kien nghi (tu catalog AuditRecommendation) cho nhieu dong TTSS. */
public record AuditTtssLinkRecommendationRequest(
        @NotEmpty List<UUID> ttssRecordIds,
        @NotNull UUID recommendationId
) {
}
