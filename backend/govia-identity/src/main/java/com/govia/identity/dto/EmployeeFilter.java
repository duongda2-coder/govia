package com.govia.identity.dto;

import com.govia.identity.entity.EmployeeStatus;

import java.util.UUID;

/**
 * Tieu chi loc/tim danh sach nhan vien - dung chung cho list va export Excel/Word.
 * Cac truong employeeCode/fullName/positionName/phone/email/orgUnitName/managerName la tim theo tung cot
 * (khop mot phan, khong phan biet hoa thuong); keyword la tim nhanh chung (fullName hoac employeeCode).
 */
public record EmployeeFilter(
        UUID orgUnitId,
        EmployeeStatus status,
        String keyword,
        String employeeCode,
        String fullName,
        String positionName,
        String phone,
        String email,
        String orgUnitName,
        String managerName
) {
}
