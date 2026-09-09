package com.govia.audit.processstepqt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AuditProcessStepDetailQtRequest(
        UUID businessSegmentId,
        UUID processStepSummaryId,
        UUID controlPointId,
        @NotBlank @Size(max = 50) String code,
        boolean active
) {
}
