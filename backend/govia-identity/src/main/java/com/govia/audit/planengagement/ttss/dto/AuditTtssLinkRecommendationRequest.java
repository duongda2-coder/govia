package com.govia.audit.planengagement.ttss.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/** "4. Gắn kiến nghị" - gan 1 hoac NHIEU kien nghi (tu catalog AuditRecommendation) cho nhieu dong TTSS
 * (test25.9: 1 TTSS gan nhieu kien nghi truong doan). Danh sach kien nghi THAY THE toan bo kien nghi
 * da gan truoc do cua tung dong duoc chon. */
public record AuditTtssLinkRecommendationRequest(
        @NotEmpty List<UUID> ttssRecordIds,
        @NotEmpty List<UUID> recommendationIds
) {
}
