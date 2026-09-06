package com.govia.audit.planengagement.recommendation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** "Thêm kiến nghị" - mã (code) do NGƯỜI DÙNG tự nhập theo quy tắc "KNKTxxx" (vd KNKT001, KNKT002...
 * xem AuditRecommendationService - KNKT000 la ma mac dinh rieng, khong duoc dat trung). */
public record AuditRecommendationRequest(
        @NotBlank @Pattern(regexp = "^KNKT\\d{3,}$", message = "Mã kiến nghị phải theo định dạng KNKTxxx (vd KNKT001)") String code,
        UUID businessSegmentId,
        @NotBlank @Size(max = 2000) String content
) {
}
