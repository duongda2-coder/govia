package com.govia.audit.planengagement.dto;

import java.util.UUID;

/** Lookup nhe cho Select "Chon truong doan" / "User ID" / "Ma truong nhom" - tranh phu thuoc quyen
 * PEOPLE.EMPLOYEE.VIEW chi vi can doc danh sach nhan vien. */
public record EmployeeOption(UUID id, String employeeCode, String fullName, String username, boolean teamLeadCapable) {
}
