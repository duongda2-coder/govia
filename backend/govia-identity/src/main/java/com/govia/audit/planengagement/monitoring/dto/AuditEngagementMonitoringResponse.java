package com.govia.audit.planengagement.monitoring.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.govia.audit.planengagement.dto.AuditEngagementResponse;

/**
 * Man hinh "Quản lý đợt kiểm toán" (nguon: "Tao CKT (2).xlsx", sheet cung ten) - bao boc
 * {@link AuditEngagementResponse} (giu nguyen toan bo field cua CKT, tra phang qua @JsonUnwrapped
 * de FE dung truc tiep nhu AuditEngagementItem) va bo sung cac so lieu TONG HOP dem/tinh tu cac
 * bang lien quan, khong luu rieng:
 * <ul>
 *   <li>memberCount = so thanh vien trong tat ca cac nhom cua CKT.</li>
 *   <li>businessSegmentCount = "Số nghiệp vụ" - so nghiep vu (BUSINESS_SEGMENT) PHAN BIET duoc
 *   giao cho cac thanh vien cua doan.</li>
 *   <li>totalFindings/totalMaterialFindings = "Tổng số phát hiện"/"Phát hiện trọng yếu" - dem tren
 *   AuditTtssRecord theo engagementId (moi dong TTSS = 1 "phat hien", material=true = trong yeu).
 *   CHU Y: KHONG dung AuditFinding (entity do phuc vu AI Agent, tach biet hoan toan voi CKT).</li>
 *   <li>recommendationCount = "Số kiến nghị" - dem AuditTtssRecord co teamRecommendationId != null
 *   (kien nghi CHINH THUC da duoc gan cho phat hien), KHONG dung so dong catalog
 *   AuditRecommendation vi catalog do chi la danh sach ma kien nghi kha dung, luon co san dong mac
 *   dinh KNKT000 nen khong phan anh dung khoi luong cong viec thuc te.</li>
 * </ul>
 */
public record AuditEngagementMonitoringResponse(
        @JsonUnwrapped AuditEngagementResponse engagement,
        int memberCount,
        int businessSegmentCount,
        int totalFindings,
        int totalMaterialFindings,
        int recommendationCount
) {
}
