package com.govia.audit.processstepqt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AuditProcessStepSummaryQtRequest(
        UUID businessSegmentId,
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 255) String name,
        UUID workItemId,
        Integer applicableYear,
        boolean active
) {
}
