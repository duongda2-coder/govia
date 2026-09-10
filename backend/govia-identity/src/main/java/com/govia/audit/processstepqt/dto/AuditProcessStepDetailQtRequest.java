package com.govia.audit.processstepqt.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AuditProcessStepDetailQtRequest(
        UUID businessSegmentId,
        UUID processStepSummaryId,
        @NotBlank @Size(max = 50) String code,
        Integer applicableYear,
        boolean active
) {
}
