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
        UUID businessSegmentId,
        UUID departmentId,
        LocalDate idIssueDate,
        String idIssuePlace
) {

    /** Constructor cu (truoc khi them ngay/noi cap CCCD/CMND) - giu nguyen cac noi goi khong quan tam den 2 truong nay. */
    public EmployeeRequest(String employeeCode, String fullName, String email, String personalEmail, String phone, UUID orgUnitId,
                           UUID positionId, LocalDate hireDate, LocalDate dateOfBirth, Gender gender, String idNumber, UUID managerId,
                           EmployeeRankLevel rankLevel, String ethnicity, String hometown, LocalDate partyJoinDate,
                           LocalDate auditDeptJoinDate, String priorWorkHistory, EmployeeEducationLevel educationLevel,
                           EmployeePoliticalLevel politicalLevel, String foreignLanguageLevel, String itSkillLevel,
                           EmployeeAuditorClassification auditorClassification, boolean teamLeadCapable, String auditedBranches,
                           String otherDuties, String relatedPersonBranches, boolean onLeave, UUID businessSegmentId, UUID departmentId) {
        this(employeeCode, fullName, email, personalEmail, phone, orgUnitId, positionId, hireDate, dateOfBirth, gender, idNumber, managerId,
                rankLevel, ethnicity, hometown, partyJoinDate, auditDeptJoinDate, priorWorkHistory, educationLevel, politicalLevel,
                foreignLanguageLevel, itSkillLevel, auditorClassification, teamLeadCapable, auditedBranches, otherDuties,
                relatedPersonBranches, onLeave, businessSegmentId, departmentId, null, null);
    }
}
