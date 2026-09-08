package com.govia.audit.planengagement.statistics.dto;

/** "1: Thống kê theo năm" - 1 dòng cho mỗi (Năm, Mã mảng nghiệp vụ). */
public record YearSegmentStatRow(
        Integer year,
        String businessSegmentCode,
        String businessSegmentName,
        long ttssCount,
        long materialTtssCount,
        long recommendationCount
) {
}
