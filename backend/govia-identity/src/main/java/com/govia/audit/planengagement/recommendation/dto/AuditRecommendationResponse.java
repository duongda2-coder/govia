package com.govia.audit.planengagement.recommendation.dto;

import java.util.UUID;

public record AuditRecommendationResponse(
        UUID id,
        UUID engagementId,
        String code,
        UUID businessSegmentId,
        String businessSegmentCode,
        String content
) {
}
