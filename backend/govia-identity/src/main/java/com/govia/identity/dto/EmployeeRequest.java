package com.govia.identity.dto;

import com.govia.identity.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.UUID;

public record EmployeeRequest(
        @NotBlank String employeeCode,
        @NotBlank String fullName,
        @Email String email,
        @Email String personalEmail,
        String phone,
        UUID orgUnitId,
        UUID positionId,
        LocalDate hireDate,
        LocalDate dateOfBirth,
        Gender gender,
        String idNumber,
        UUID managerId
) {
}
