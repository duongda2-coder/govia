package com.govia.audit.planengagement.dto;

import com.govia.audit.planengagement.entity.AuditEngagementStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/** Mã CKT KHÔNG có trong request - server tự sinh khi tạo, bất biến khi sửa. */
public record AuditEngagementRequest(
        @NotNull UUID auditObjectUnitId,
        @NotNull Integer year,
        @NotNull Integer expectedMonth,
        @NotNull LocalDate decisionDate,
        @NotNull UUID teamLeadEmployeeId,
        @jakarta.validation.constraints.NotBlank @Size(max = 50) String decisionNumber,
        AuditEngagementStatus status,
        @Size(max = 20) String riskRank,
        @Size(max = 255) String name,
        @Size(max = 4000) String objective,
        @Size(max = 4000) String scope,
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
