package com.govia.audit.planengagement.monitoring.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * 1 dong cua bang "Chi tiết đoàn kiểm toán" (nut "Chi tiet doan KT" tren man hinh "Quan ly dot
 * kiem toan"). "Chức danh" (roleTitle) khong luu rieng - suy ra: employeeId ==
 * AuditEngagement.teamLeadEmployeeId => "Trưởng đoàn", employeeId == AuditEngagementGroup.
 * leaderEmployeeId cua nhom minh => "Trưởng nhóm", con lai => "Thành viên".
 * <p>
 * totalFindings/ttssTypeCount/totalMaterialFindings/materialTtssTypeCount/recommendationCount deu
 * dem tren AuditTtssRecord co ttssPerformerName KHOP (theo ten day du, khong phan biet hoa
 * thuong) voi thanh vien nay - day la truong duy nhat tren AuditTtssRecord xac dinh "can bo thuc
 * hien TTSS" (tu dong = nguoi upload, xem AuditTtssRecord.ttssPerformerName). "Loai TTSS"
 * (ttssTypeCount) = so ma phat hien (findingCode) PHAN BIET, khac voi totalFindings la tong so
 * dong (co the trung ma qua nhieu lan upload).
 */
public record AuditEngagementTeamMemberDetailResponse(
        int stt,
        UUID memberId,
        UUID employeeId,
        String employeeCode,
        String employeeName,
        String roleTitle,
        String businessSegmentNames,
        int totalFindings,
        int ttssTypeCount,
        int totalMaterialFindings,
        int materialTtssTypeCount,
        int recommendationCount,
        ProgressStat cbktProgress,
        ProgressStat thktSampleProgress,
        ProgressStat thktNoSampleProgress,
        BigDecimal score,
        String ranking,
        String note
) {
}
