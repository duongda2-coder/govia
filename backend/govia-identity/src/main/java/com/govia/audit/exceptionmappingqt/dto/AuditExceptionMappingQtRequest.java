package com.govia.audit.exceptionmappingqt.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AuditExceptionMappingQtRequest(
        UUID businessSegmentId,
        @NotNull UUID processStepDetailId,
        @NotNull UUID exceptionTypeId,
        boolean active
) {
}
