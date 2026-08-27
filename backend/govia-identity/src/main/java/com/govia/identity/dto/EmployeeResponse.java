package com.govia.identity.dto;

import com.govia.identity.entity.EmployeeAuditorClassification;
import com.govia.identity.entity.EmployeeEducationLevel;
import com.govia.identity.entity.EmployeePoliticalLevel;
import com.govia.identity.entity.EmployeeRankLevel;
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
        UUID businessSegmentId,
        String businessSegmentCode,
        String businessSegmentName,
        /** Username tai khoan dang nhap gan voi nhan vien nay - null neu chua co tai khoan. */
        String username,
        Instant createdAt,
        Instant updatedAt
) {
}
