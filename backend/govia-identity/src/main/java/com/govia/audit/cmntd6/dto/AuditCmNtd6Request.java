package com.govia.audit.cmntd6.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AuditCmNtd6Request(
        @NotNull UUID engagementId,
        UUID assignedEmployeeId,
        UUID processStepSummaryId,
        @NotBlank @Size(max = 10) String branchCode,
        @Size(max = 50) String staffCode,
        @NotBlank @Size(max = 100) String staffName,
        @NotBlank @Size(max = 20) String ipcasUser,
        @Size(max = 20) String adUser,
        @Size(max = 20) String securityDevice,
        @Size(max = 1000) String sampleReason,
        @Size(max = 20) String sampleCode,
        @Size(max = 120) String auditResult,
        boolean active
) {
}
