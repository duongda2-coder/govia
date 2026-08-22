package com.govia.identity.dto;

import com.govia.identity.entity.EmployeeStatus;
import com.govia.identity.entity.Gender;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EmployeeResponse(
        UUID id,
        String employeeCode,
        String fullName,
        String email,
        String personalEmail,
        String phone,
        UUID orgUnitId,
        String orgUnitCode,
        String orgUnitName,
        UUID positionId,
        String positionCode,
        String positionName,
        LocalDate hireDate,
        EmployeeStatus status,
        LocalDate dateOfBirth,
        Gender gender,
        String idNumber,
        UUID managerId,
        String managerCode,
        String managerName,
        /** Username tai khoan dang nhap gan voi nhan vien nay - null neu chua co tai khoan. */
        String username,
        Instant createdAt,
        Instant updatedAt
) {
}
