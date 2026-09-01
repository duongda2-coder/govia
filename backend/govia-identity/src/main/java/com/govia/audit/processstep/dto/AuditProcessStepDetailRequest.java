package com.govia.audit.processstep.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AuditProcessStepDetailRequest(
        UUID businessSegmentId,
        UUID processStepSummaryId,
        @NotBlank @Size(max = 50) String code,
        boolean active
) {
}
