package com.govia.audit.planengagement.dto;

import java.util.List;

/** Cac ma nhom hop le de tao moi cho 1 CKT - dung boi man hinh "Danh sach nhom" (Select "Ten nhom")
 * va "Danh sach thanh vien" (loc "Nghiep vu" theo dung 1 ma khi processScoped=true). Voi CKT thuong:
 * codes = 3 ma co dinh (DIEUHANH/NTINDUNG/TINDUNG), processScoped=false. Voi CKT quy trinh (co
 * processEngagementId): codes = [ma nghiep vu cua CKT quy trinh cha] (vd "AM"), processScoped=true -
 * xem AuditEngagementTeamService.validGroupCodes(). */
public record AuditEngagementGroupCodeOptionsResponse(boolean processScoped, List<String> codes) {
}
