package com.govia.audit.exceptionmapping.dto;

import java.util.UUID;

public record AuditExceptionMappingResponse(
        UUID id,
        UUID businessSegmentId,
        String businessSegmentCode,
        String businessSegmentName,
        UUID processStepDetailId,
        String processStepDetailCode,
        UUID exceptionTypeId,
        String exceptionTypeCode,
        String exceptionTypeName,
        boolean active
) {
}
