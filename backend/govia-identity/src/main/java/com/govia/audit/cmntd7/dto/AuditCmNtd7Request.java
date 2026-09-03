package com.govia.audit.cmntd7.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AuditCmNtd7Request(
        @NotNull UUID engagementId,
        UUID assignedEmployeeId,
        UUID processStepSummaryId,
        @NotBlank @Size(max = 10) String branchCode,
        @NotBlank @Size(max = 10) String constructionCode,
        @Size(max = 50) String constructionName,
        @Size(max = 120) String content,
        @Size(max = 20) String documentType,
        @Size(max = 120) String completenessAssessment,
        @Size(max = 250) String assessment,
        @Size(max = 120) String auditResult,
        boolean active
) {
}
