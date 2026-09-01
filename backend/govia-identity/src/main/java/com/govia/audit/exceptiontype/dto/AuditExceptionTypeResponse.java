package com.govia.audit.exceptiontype.dto;

import com.govia.audit.exceptiontype.entity.AuditExceptionCategory;
import com.govia.audit.masterdata.entity.AuditLevel;

import java.util.UUID;

public record AuditExceptionTypeResponse(
        UUID id,
        UUID businessSegmentId,
        String businessSegmentCode,
        String businessSegmentName,
        String code,
        String name,
        AuditExceptionCategory category,
        AuditLevel impactLevel,
        String classificationBasis,
        boolean active
) {
}
