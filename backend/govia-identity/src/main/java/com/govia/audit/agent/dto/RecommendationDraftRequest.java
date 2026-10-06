package com.govia.audit.agent.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Yeu cau "AI soan kien nghi" tu man hinh "Thêm kiến nghị" cua 1 CKT. businessSegmentId: nghiep vu
 * dang chon tren form (tuy chon). instruction: yeu cau them cua nguoi dung (tuy chon, vd "ngan gon"). */
public record RecommendationDraftRequest(
        @NotNull UUID engagementId,
        UUID businessSegmentId,
        @Size(max = 500) String instruction
) {
}
