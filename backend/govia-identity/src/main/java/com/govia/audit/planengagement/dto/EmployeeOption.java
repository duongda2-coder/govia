package com.govia.audit.planengagement.dto;

import java.util.List;
import java.util.UUID;

/**
 * Lookup nhe cho Select "Chon truong doan" / "User ID" / "Ma truong nhom" - tranh phu thuoc quyen
 * PEOPLE.EMPLOYEE.VIEW chi vi can doc danh sach nhan vien. truongDoanCapable/truongNhomCapable lay
 * tu danh muc "Kha nang dam nhan linh vuc" (AuditEmployeeCapability, sheet ZTC_KNDN) - KHONG con
 * dung Employee.teamLeadCapable (truong rieng tren form Nhan vien, nhung khong lien quan toi 2 co
 * nay nen luon rong/sai muc dich khi dung lam nguon cho danh sach chon Truong doan/Truong nhom).
 * capableSegmentCodes: ma danh muc "Nghiep vu" (BUSINESS_SEGMENT, vd "GA", "DP", "LN"...) tuong ung
 * voi tung co "kha nang dam nhan" = true cua nhan vien nay - dung de loc "Nghiep vu" khi them thanh
 * vien nhom (xem bang dich co dinh trong AuditEngagementService.capableSegmentCodes()).
 */
public record EmployeeOption(UUID id, String employeeCode, String fullName, String username,
                              boolean truongDoanCapable, boolean truongNhomCapable, List<String> capableSegmentCodes) {
}
