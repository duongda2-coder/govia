package com.govia.audit.planengagement.progressreport.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record AuditProgressReportApproveRequest(
        @NotEmpty List<UUID> reportIds
) {
}
