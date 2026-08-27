package com.govia.identity.dto;

import com.govia.identity.entity.EmployeeAuditorClassification;
import com.govia.identity.entity.EmployeeEducationLevel;
import com.govia.identity.entity.EmployeePoliticalLevel;
import com.govia.identity.entity.EmployeeRankLevel;
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
        UUID managerId,
        EmployeeRankLevel rankLevel,
        String ethnicity,
        String hometown,
        LocalDate partyJoinDate,
        LocalDate auditDeptJoinDate,
        String priorWorkHistory,
        EmployeeEducationLevel educationLevel,
        EmployeePoliticalLevel politicalLevel,
        String foreignLanguageLevel,
        String itSkillLevel,
        EmployeeAuditorClassification auditorClassification,
        boolean teamLeadCapable,
        String auditedBranches,
        String otherDuties,
        String relatedPersonBranches,
        boolean onLeave,
        UUID businessSegmentId
) {
}
