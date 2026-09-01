package com.govia.audit.exceptionmapping.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AuditExceptionMappingRequest(
        UUID businessSegmentId,
        @NotNull UUID processStepDetailId,
        @NotNull UUID exceptionTypeId,
        boolean active
) {
}
