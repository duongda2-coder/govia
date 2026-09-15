package com.govia.audit.khkt.common.entity;

/** Cot "Trang thai" cua BP2/TH2 (Phan 2, da xac nhan) - sheet ghi chu "Them nut phe duyet va chua
 * phe duyet". Moi dong bat dau PENDING khi vua "Xac nhan danh sach", chuyen APPROVED qua nut phe
 * duyet rieng (xem AuditKhktBpService/AuditKhktThService#setApprovalStatus). */
public enum AuditKhktApprovalStatus {
    PENDING,
    APPROVED
}
