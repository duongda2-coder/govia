package com.govia.audit.planengagement.statistics.dto;

/** "2: Thống kê theo CKT" - 1 dòng cho mỗi (Mã CKT, Mã mảng nghiệp vụ). */
public record EngagementSegmentStatRow(
        String engagementCode,
        String businessSegmentCode,
        String businessSegmentName,
        long ttssCount,
        long materialTtssCount,
        long recommendationCount
) {
}
