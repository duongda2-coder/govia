package com.govia.audit.planengagement.processengagement.dto;

import java.time.LocalDate;
import java.util.UUID;

public record AuditProcessEngagementResponse(
        UUID id,
        String code,
        String objectType,
        UUID businessSegmentId,
        String businessSegmentCode,
        String businessSegmentName,
        Integer year,
        Integer expectedMonth,
        LocalDate decisionDate,
        UUID teamLeadEmployeeId,
        String teamLeadEmployeeCode,
        String teamLeadEmployeeName,
        String teamLeadUsername,
        String decisionNumber,
        String name,
        String workSetCode,
        String createdBy,
        int childCount,
        int memberCount
) {
}
