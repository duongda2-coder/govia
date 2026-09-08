package com.govia.audit.planengagement.statistics.dto;

/** "Chi tiết theo chi nhánh" - drill-down cua UnitStatRow, 1 dòng cho mỗi (Số QĐ, Năm) - thuc chat
 * la 1 cuoc kiem toan (AuditEngagement) thuoc chi nhanh dang xem. */
public record UnitDetailStatRow(
        String decisionNumber,
        Integer year,
        long ttssCount,
        long materialTtssCount,
        long recommendationCount,
        long completedRecommendationCount,
        String riskRank
) {
}
