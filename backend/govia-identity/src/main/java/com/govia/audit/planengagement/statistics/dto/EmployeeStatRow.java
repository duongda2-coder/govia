package com.govia.audit.planengagement.statistics.dto;

/** "3: Thống kê theo thành viên đoàn" - 1 dòng cho mỗi (Cán bộ, Năm). Cán bộ được xác định qua
 * AuditTtssRecord.recordUsername (nguoi upload) -> UserAccount -> Employee, vi AuditTtssRecord
 * KHONG co cot assignedEmployeeId rieng (xem AuditTtssRecord.java). */
public record EmployeeStatRow(
        String employeeUsername,
        String employeeCode,
        String employeeName,
        Integer year,
        long ttssCount,
        long materialTtssCount,
        long recommendationCount
) {
}
