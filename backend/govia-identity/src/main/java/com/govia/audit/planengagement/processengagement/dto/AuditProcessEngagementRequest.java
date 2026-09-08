package com.govia.audit.planengagement.processengagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/** Ma CKT KHONG co trong request - server tu sinh khi tao, bat bien khi sua. */
public record AuditProcessEngagementRequest(
        @NotNull UUID businessSegmentId,
        @NotNull Integer year,
        @NotNull Integer expectedMonth,
        @NotNull LocalDate decisionDate,
        @NotNull UUID teamLeadEmployeeId,
        @NotBlank @Size(max = 50) String decisionNumber,
        @Size(max = 255) String name
) {
}
