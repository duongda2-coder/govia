package com.govia.audit.planengagement.dto;

import com.govia.audit.planengagement.entity.AuditEngagementStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record AuditEngagementResponse(
        UUID id,
        String code,
        UUID auditObjectUnitId,
        String auditObjectUnitCode,
        String auditObjectUnitName,
        String unitType,
        Integer year,
        Integer expectedMonth,
        LocalDate decisionDate,
        UUID teamLeadEmployeeId,
        String teamLeadEmployeeCode,
        String teamLeadEmployeeName,
        String decisionNumber,
        AuditEngagementStatus status,
        String riskRank,
        String name,
        String objective,
        String scope,
        LocalDate planningStartDate,
        LocalDate planningEndDate,
        LocalDate fieldworkStartDate,
        LocalDate fieldworkEndDate,
        LocalDate reportStartDate,
        LocalDate reportEndDate,
        LocalDateTime infoCollectionStart,
        LocalDateTime infoCollectionEnd,
        LocalDateTime sampleRequestStart,
        LocalDateTime sampleRequestEnd,
        LocalDateTime reportPlanStart,
        LocalDateTime reportPlanEnd
) {
}
