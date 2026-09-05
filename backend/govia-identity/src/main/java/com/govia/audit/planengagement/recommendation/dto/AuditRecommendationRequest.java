package com.govia.audit.planengagement.recommendation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** "Thêm kiến nghị" - mã (code) do hệ thống tự sinh (KNKT001, KNKT002...), không nhận từ client. */
public record AuditRecommendationRequest(
        UUID businessSegmentId,
        @NotBlank @Size(max = 2000) String content
) {
}
