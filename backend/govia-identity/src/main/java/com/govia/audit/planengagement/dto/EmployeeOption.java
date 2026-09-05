package com.govia.audit.planengagement.dto;

import java.util.List;
import java.util.UUID;

/**
 * Lookup nhe cho Select "Chon truong doan" / "User ID" / "Ma truong nhom" - tranh phu thuoc quyen
 * PEOPLE.EMPLOYEE.VIEW chi vi can doc danh sach nhan vien. truongDoanCapable/truongNhomCapable lay
 * tu danh muc "Kha nang dam nhan linh vuc" (AuditEmployeeCapability, sheet ZTC_KNDN) - KHONG con
 * dung Employee.teamLeadCapable (truong rieng tren form Nhan vien, nhung khong lien quan toi 2 co
 * nay nen luon rong/sai muc dich khi dung lam nguon cho danh sach chon Truong doan/Truong nhom).
 * capableSegmentCodes: cac ma linh vuc (THE, QTDH, HDV, TCKT, CNTT, TTKQ, PCRT, TTQT, XDCB, TD) ma
 * nhan vien nay co co "kha nang dam nhan" = true - dung de loc "Nghiep vu" khi them thanh vien nhom.
 */
public record EmployeeOption(UUID id, String employeeCode, String fullName, String username,
                              boolean truongDoanCapable, boolean truongNhomCapable, List<String> capableSegmentCodes) {
}
