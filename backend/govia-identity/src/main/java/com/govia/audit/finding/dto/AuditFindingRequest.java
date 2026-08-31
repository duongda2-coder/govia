package com.govia.audit.finding.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record AuditFindingRequest(
        @NotBlank @Size(max = 10) String branchCode,
        @NotBlank @Size(max = 500) String title,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 50) String severity,
        @NotNull LocalDate detectedDate,
        boolean active
) {
}
