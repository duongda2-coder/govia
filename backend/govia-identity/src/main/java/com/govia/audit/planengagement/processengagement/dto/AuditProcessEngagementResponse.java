package com.govia.audit.planengagement.processengagement.dto;

import java.time.LocalDate;
import java.util.UUID;

public record AuditProcessEngagementResponse(
        UUID id,
        String code,
        UUID businessSegmentId,
        String businessSegmentCode,
        String businessSegmentName,
        Integer year,
        Integer expectedMonth,
        LocalDate decisionDate,
        UUID teamLeadEmployeeId,
        String teamLeadEmployeeCode,
        String teamLeadEmployeeName,
        String decisionNumber,
        String name,
        String workSetCode
) {
}
