package com.govia.audit.exceptionmappingqt.dto;

import java.util.UUID;

public record AuditExceptionMappingQtResponse(
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
