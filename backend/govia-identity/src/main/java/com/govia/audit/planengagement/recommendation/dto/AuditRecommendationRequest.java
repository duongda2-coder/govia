package com.govia.audit.planengagement.recommendation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** "Thêm kiến nghị" - mã (code) TU SINH o server theo quy tac "KNKTxxx" (xem
 * AuditRecommendationService.generateNextCode()), nguoi dung KHONG con tu nhap ma nua (truoc day
 * bat go tay, doi lai theo yeu cau nguoi dung - xem lich su commit). */
public record AuditRecommendationRequest(
        UUID businessSegmentId,
        @NotBlank @Size(max = 2000) String content
) {
}
