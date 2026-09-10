package com.govia.audit.exceptiontypeqt.dto;

import com.govia.audit.exceptiontype.entity.AuditExceptionCategory;
import com.govia.audit.masterdata.entity.AuditLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AuditExceptionTypeQtRequest(
        UUID businessSegmentId,
        @NotBlank @Size(max = 20) String code,
        @NotBlank @Size(max = 500) String name,
        AuditExceptionCategory category,
        AuditLevel impactLevel,
        @Size(max = 255) String classificationBasis,
        Integer applicableYear,
        boolean active
) {
}
