package com.govia.audit.processstep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AuditProcessStepSummaryRequest(
        UUID businessSegmentId,
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 255) String name,
        UUID workItemId,
        boolean active
) {
}
