package com.govia.audit.workitemqt.dto;

import com.govia.audit.workitem.entity.AuditWorkPhase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AuditWorkItemQtRequest(
        UUID auditObjectCategoryId,
        AuditWorkPhase phase,
        UUID businessSegmentId,
        @NotBlank @Size(max = 10) String code,
        @NotBlank @Size(max = 1000) String name,
        @NotNull Integer applicableYear,
        @NotBlank @Size(max = 50) String workSetCode,
        String workType,
        boolean active,
        boolean hasSampleSelection,
        @Size(max = 10) String branchOrHeadOffice
) {
}
